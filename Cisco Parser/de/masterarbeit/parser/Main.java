package de.masterarbeit.parser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.masterarbeit.modell.*;
import static de.masterarbeit.toolbox.Toolbox.*;

/**
 * Main Klasse des Parsers für die Cisco Konfigurationsdateien des Housing Centers. Die Klasse besteht aus einer großen
 * Main-Methode, die die gesamte Verarbeitung steuert. Kommentare zu dein einzelnen Schritten finden sich im Quelltext.
 * 
 * @author Marcel Schuster
 *
 */
public class Main {

	/**
	 * Steuerung der gesamten Verarbeitung. Weitere Kommentare finden sich im Quelltext.
	 * 
	 * Achtung: Vor einer Ausführung müssen die Pfade angepasst werden, die am Anfang der Main zu finden sind!
	 * 
	 * @param args
	 *            Argumente werden nicht verarbeitet
	 * @throws IOException
	 *             Stumpfe Weiterleitung der Exception
	 */
	public static void main(String[] args) throws IOException {
		// ----------------------------------------------------------------------------
		// KONFIGURATION START
		// ----------------------------------------------------------------------------
		// Pfad, in dem sich alle Dateien befinden. Auf abschließenden (Back-)Slash achten!
		String path = "<TBD>";
		String outputFile = "<TBD>.soil";
		String[][] components = new String[][] { { "<Running-Config>.txt", "<CDP>.txt" } };
		// ----------------------------------------------------------------------------
		// KONFIGURATION ENDE
		// ----------------------------------------------------------------------------

		long startTime = System.currentTimeMillis();
		int count_l1_links = 0;
		int count_l2_links = 0;

		System.out.println("=== Reading configuration files..." + "\n");

		// Liste von Netzkomponenten initialisieren und alle Konfigurations- und CDP-Dateien einlesen
		List<NetworkComponent> networkComponents = new ArrayList<>();
		for (String[] component : components) {
			networkComponents.add(new NetworkComponent(path + component[0], path + component[1]));
		}

		System.out.println("=== Processing links from CDP information..." + "\n");

		// [Schritt 1.1] Interfaces aus den CDP Informationen herausfinden, die für die Verbindung der Komponenten
		// untereinander verantwortlich sind.
		// [Schritt 1.2] Links für Layer1- und Layer2Interfaces erstellen lassen. Die Verarbeitung passiert auf Grundlage der
		// CDP-Informationen. Daher werden die VSL-Interfaces zwar mit ausgegeben, jedoch wird dafür die Link-Verarbeitung
		// nicht durchgeführt.
		for (NetworkComponent srcNC : networkComponents) {
			for (NetworkComponent dstNC : networkComponents) {
				List<List<String>> neighbors = srcNC.cdpResult.findNeighbors("^.*(" + dstNC.name + ").*$");
				Map<String, String> interfaces = getInterfaceNamesToNeighbors(neighbors);

				// Filter auf Basis der CDP Informationen füllen (Interfaces für Kommunikation untereinander)
				srcNC.layer1InterfaceFilter.addAll(interfaces.keySet());

				// Layer1Links verarbeiten (Layer1Interfaces verknüpfen)
				for (Map.Entry<String, String> i : interfaces.entrySet()) {
					Layer1Interface srcL1Int = srcNC.layer1Interfaces.get(i.getKey());
					Layer1Interface dstL1Int = dstNC.layer1Interfaces.get(i.getValue());

					// Prüfung, ob Link bereits existiert (verhindert doppelte Verarbeitung)
					if (srcL1Int != null && dstL1Int != null && !srcL1Int.linkProcessed && !dstL1Int.linkProcessed) {
						// Layer1Interfaces verlinken
						srcL1Int.layer1LinkTo = dstL1Int;
						dstL1Int.layer1LinkTo = srcL1Int;

						// Layer1Interfaces als verarbeitet markieren
						srcL1Int.linkProcessed = true;
						dstL1Int.linkProcessed = true;
						count_l1_links++;

						// Verknüpfung der Layer2Links auf Grundlage der zuvor erstellten Layer1Links Alle Layer2Interfaces
						// abrufen in logisch-chronologischer Reihenfolge
						for (Layer2Interface srcL2Int : srcL1Int.getAllLayer2Interfaces()) {
							for (Layer2Interface dstL2Int : dstL1Int.getAllLayer2Interfaces()) {

								// Wenn Layer2Interfaces noch nicht verarbeitet und VLANs sich überschneiden ODER beide
								// Interfaces keinen expliziten Typ haben (dann senden sie sich ungetaggte Frames)
								if (!srcL2Int.linkProcessed && !dstL2Int.linkProcessed
										&& (srcL2Int.vlanIntersection(dstL2Int)
												|| (srcL2Int.interfaceType == null && dstL2Int.interfaceType == null))) {

									// Layer2Interfaces verlinken
									srcL2Int.layer2LinksTo.put(dstL2Int.name, dstL2Int);
									dstL2Int.layer2LinksTo.put(srcL2Int.name, srcL2Int);

									// Layer2Interfaces als verarbeitet markieren
									srcL2Int.linkProcessed = true;
									dstL2Int.linkProcessed = true;
									count_l2_links++;

									// Alle "logisch untergeordneten Interfaces" auch als verarbeitet markieren. Das klappt
									// pauschal so, da die Mengen ggf. leer sind!
									// Hiermit ist nicht die Tagged <-> Untagged Assoziation gemeint!
									List<Layer2Interface> tmpL2Interfaces = new ArrayList<>();
									tmpL2Interfaces.addAll(srcL2Int.aggregationInterfaces.values());
									tmpL2Interfaces.addAll(dstL2Int.aggregationInterfaces.values());

									for (Layer2Interface tmpL2Interface : tmpL2Interfaces) {
										tmpL2Interface.linkProcessed = true;
									}
								}
							}
						}
					}
				}
			}
		}

		System.out.println("=== Merge Interfaces into LAGs..." + "\n");

		// [Schritt 2] LAGs verschmelzen lassen nach obiger Vorarbeit mit der Linkerstellung
		for (NetworkComponent srcNC : networkComponents) {
			for (Layer1Interface srcL1Int : srcNC.layer1Interfaces.values()) {
				Layer1Interface dstL1Int = srcL1Int.layer1LinkTo;

				// Wenn es ein Ziel gibt und der Merge noch nicht verarbeitet wurde
				if (dstL1Int != null && !srcL1Int.mergeProcessed && !dstL1Int.mergeProcessed) {
					NetworkComponent dstNC = dstL1Int.networkComponent;

					// Alle Layer2Interfaces logisch von oben nach unten durchgehen (ich gehe hier _nicht_ davon aus, dass
					// PeerLinks Aggregator Interfaces sind!)
					for (Layer2Interface srcL2Int : srcL1Int.getAllLayer2Interfaces()) {
						for (Layer2Interface dstL2Int : dstL1Int.getAllLayer2Interfaces()) {

							// Wenn beide Layer2Interfaces mit einem PeerLink verbunden sind (in beide Richtungen!)
							if (srcL2Int.peerLink != null && dstL2Int.peerLink != null
									&& srcL2Int.layer2LinksTo.containsValue(dstL2Int)
									&& dstL2Int.layer2LinksTo.containsValue(srcL2Int)) {

								// Annahme: Nur Aggregator Interfaces verschmelzen!
								for (Layer2Interface srcAggL2Int : srcNC.aggregatorInterfaces.values()) {
									for (Layer2Interface dstAggL2Int : dstNC.aggregatorInterfaces.values()) {

										// Wenn die VPC Nummern übereinstimmen, ist es dasselbe Interface!
										if (srcAggL2Int.vpc != null && srcAggL2Int.vpc.equals(dstAggL2Int.vpc)) {
											srcAggL2Int.mergedInterface = dstAggL2Int;
											dstAggL2Int.mergedInterface = srcAggL2Int;
										}
									}
								}

								// Alle assoziierten Layer1Interfaces als verarbeitet markieren
								List<Layer1Interface> tmpL1Interfaces = new ArrayList<>();
								tmpL1Interfaces.addAll(srcL2Int.layer1Interfaces.values());
								tmpL1Interfaces.addAll(dstL2Int.layer1Interfaces.values());

								for (Layer1Interface tmpL1Interface : tmpL1Interfaces) {
									tmpL1Interface.mergeProcessed = true;
								}
							}
						}
					}
				}
			}
		}

		System.out.println("=== Generating SOIL-Output..." + "\n");

		List<String> layer1Links = new ArrayList<>();
		List<String> layer2Links = new ArrayList<>();

		// [Schritt 3] SOIL-Ausgaben der Layer1Links und Layer2Links generieren
		for (NetworkComponent srcNC : networkComponents) {
			for (Layer1Interface srcL1Int : srcNC.layer1Interfaces.values()) {
				Layer1Interface dstL1Int = srcL1Int.layer1LinkTo;

				// Wenn das aktuelle Layer1Interface einen Link hat und zudem beide Interface noch nicht verarbeitet wurden
				if (dstL1Int != null && !srcL1Int.linkSoilProcessed && !dstL1Int.linkSoilProcessed) {
					NetworkComponent dstNC = dstL1Int.networkComponent;

					// SOIL-Ausgaben generieren
					layer1Links.add("!l1l := new Layer1Link");
					layer1Links.add("!i1 := NetworkComponent.allInstances()->any(p | p.name = '" + srcNC.name
							+ "').getLayer1Interfaces()->any(i | i.name='" + srcL1Int.name + "')");
					layer1Links.add("!i2 := NetworkComponent.allInstances()->any(p | p.name = '" + dstNC.name
							+ "').getLayer1Interfaces()->any(i | i.name='" + dstL1Int.name + "')");
					layer1Links.add("!insert (i1, l1l) into HasLayer1Link");
					layer1Links.add("!insert (i2, l1l) into HasLayer1Link");
					layer1Links.add("");

					// Verarbeitung der SOIL-Ausgabe für beide Layer1Interfaces abhaken
					srcL1Int.linkSoilProcessed = true;
					dstL1Int.linkSoilProcessed = true;

					// Alle assoziierten Layer2Interfaces bearbeiten
					for (Layer2Interface srcL2Int : srcL1Int.getAllLayer2Interfaces()) {

						// Menge ist ggf. leer, sofern das Interface keine Links hat.
						for (Layer2Interface dstL2Int : srcL2Int.layer2LinksTo.values()) {

							// Sofern SOIL-Ausgabe der Layer2Interfaces noch nicht verarbeitet wurde
							if (!srcL2Int.linkSoilProcessed && !dstL2Int.linkSoilProcessed) {
								layer2Links.add("!l2l := new Layer2Link");

								// Peer Links gesondert markieren
								if (srcL2Int.peerLink != null && dstL2Int.peerLink != null) {
									layer2Links.add("!l2l.peerLink := true");
								}

								// SOIL-Ausgaben generieren
								layer2Links.add("!i1 := NetworkComponent.allInstances()->any(p | p.name = '" + srcNC.name
										+ "').getLayer2Interfaces()->any(i | i.name='" + srcL2Int.name + "')");
								layer2Links.add("!i2 := NetworkComponent.allInstances()->any(p | p.name = '" + dstNC.name
										+ "').getLayer2Interfaces()->any(i | i.name='" + dstL2Int.name + "')");
								layer2Links.add("!insert (i1, l2l) into HasLayer2Links");
								layer2Links.add("!insert (i2, l2l) into HasLayer2Links");
								layer2Links.add("");

								// Verarbeitung der SOIL-Ausgabe für beide Layer2Interfaces abhaken
								srcL2Int.linkSoilProcessed = true;
								dstL2Int.linkSoilProcessed = true;

								// Sofern Layer2Interface verschmolzen ist, markiere dessen Partner auch als bearbeitet
								if (srcL2Int.mergedInterface != null) {
									srcL2Int.mergedInterface.linkSoilProcessed = true;
								}

								// Sofern Layer2Interface verschmolzen ist, markiere dessen Partner auch als bearbeitet
								if (dstL2Int.mergedInterface != null) {
									dstL2Int.mergedInterface.linkSoilProcessed = true;
								}
							}
						}
					}
				}
			}
		}

		// [Schritt 4] Alle SOIL Informationen sammeln und in eine große Datei schreiben
		List<String> fullSOIL = new ArrayList<>();

		// SOIL-Ausgaben der Netzkomponenten sammeln
		for (NetworkComponent networkComponent : networkComponents) {
			fullSOIL.addAll(networkComponent.getClassSOIL());
		}

		// SOIL-Ausgaben der Layer1- und Layer2Links einsammeln
		fullSOIL.addAll(layer1Links);
		fullSOIL.addAll(layer2Links);

		// SOIL-Ausgabe in Datei schreiben
		Files.write(Paths.get(outputFile), fullSOIL, Charset.defaultCharset());

		// Kleine Statistik zu vearbeiteten Objekten und benötigter Zeit augeben
		System.out.println("Layer1Links created: " + count_l1_links);
		System.out.println("Layer2Links created: " + count_l2_links);

		System.out.println();
		long duration = System.currentTimeMillis() - startTime;
		System.out.println("=== Done! (Duration: " + duration + " ms)");
	}
}
