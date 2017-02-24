package de.masterarbeit.modell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import de.masterarbeit.cisco.*;
import static de.masterarbeit.toolbox.Toolbox.*;

/**
 * Einfache Repräsentation einer Netzkomponente einer Netztopologie. Bei Initialisierung des Objekts wird die Running Config
 * geparst und es werden die entsprechenden Objekte erstellt.
 * 
 * Es werden alle Objekte immer doppelseitig miteinander verknüpft. Dies muss nicht sinnvoll sein, erspart aber an der einen
 * oder anderen Stelle eine unnötige Interation durch Listen.
 * 
 * @author Marcel Schuster
 *
 */
public class NetworkComponent {
	public String name;
	public boolean vss = false;
	public JCiscoConfParse runningConfig;
	public JCiscoCDPParse cdpResult;

	// Liste von Interfacenamen, die am Schluss wirklich ausgegeben werden sollen. Die Liste in der Main Methode anhand der
	// CDP-Informationen gefüllt, sodass nur die Interfaces verarbeitet werden, die für eine Verbindung untereinander sorgen.
	public List<String> layer1InterfaceFilter = new ArrayList<>();

	public Map<String, Layer1Interface> layer1Interfaces = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	// Bei den layer2Interfaces kann es sich auch um Aggregation Interfaces (Rollenname) oder Tagged Interfaces handeln!
	public Map<String, Layer2Interface> layer2Interfaces = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	// Die Aggregator Interfaces werden in einer separaten Liste gespeichert, was die Ausgabe erleichtert
	public Map<String, Layer2Interface> aggregatorInterfaces = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	public Map<String, Layer3Interface> layer3Interfaces = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	/**
	 * Klassenkonstruktur, der bei Initialisierung des Objekts direkt die Running Config parst. Die CDP-Informationen werden
	 * hingegen lediglich zwischengespeichert.
	 * 
	 * Die Verarbeitung an sich kann leider schlecht vereinfacht werden, da für jeden Interface-Typ verschiedene Regex
	 * notwendig sind.
	 * 
	 * @param runningConfig
	 *            Running Config der Komponente, die geparst wird
	 * @param cdpResult
	 *            CDP-Informationen der Komponente (wird nicht geparst, sondern nur zwischengespeichert)
	 * @throws IOException
	 *             Stumpfe Weiterleitung der Exception
	 */
	public NetworkComponent(String runningConfig, String cdpResult) throws IOException {
		this.runningConfig = new JCiscoConfParse(runningConfig);
		this.cdpResult = new JCiscoCDPParse(cdpResult);

		// Alle benötigten Regex Pattern definieren
		String search_pattern_hostname = "^hostname[\\s]?(.*)$";
		String search_pattern_portchannel = "^interface port-channel[\\d]+$";
		String search_pattern_po_sub = "^interface port-channel[\\d]+\\.[\\d]+$";
		String search_pattern_interface = "^interface ((Ten)?Gigabit)?Ethernet[\\d]+(?:\\/[\\d]+)+$";
		String search_pattern_int_sub = "^interface ((Ten)?Gigabit)?Ethernet[\\d]+(?:\\/[\\d]+)*(?:\\/[\\d]+\\.[\\d]+)$";

		// Weitere Regex Pattern definieren
		String pattern_hostname = "^hostname[\\s]?(.+)";
		String pattern_int_name = "^interface[\\s]?(.+)";
		String pattern_int_desc = "^[\\s]+description[\\s]?(.+)";
		String pattern_int_chgroup = "^[\\s]+channel-group[\\s]?([\\d]+)";
		String pattern_int_vpc = "^[\\s]+vpc[\\s]?([\\d]+)";
		String pattern_int_peerlink = "^[\\s]+vpc[\\s]?(peer-link)";
		String pattern_int_vlan = "^[\\s]+switchport[\\s]?(access|trunk)[\\s]?(?:allowed)?[\\s]?vlan[\\s]?(?:add)?[\\s]?([\\d,-]+)";
		String pattern_int_vlan_enc = "^[\\s]+(encapsulation)[\\s]?dot1q[\\s]?([\\d]+)";
		String pattern_int_ip_sub = "^[\\s]+ip address[\\s]?((?:[\\d]{1,3}\\.){3}[\\d]{1,3})[\\s]?([\\d\\./]+)";
		String pattern_int_vsl = "^[\\s]+switch virtual link[\\s]?([\\d]+)";

		// Counter für Statistik am Ende initialisieren
		int count_int = 0;
		int count_int_subint = 0;
		int count_po = 0;
		int count_po_subint = 0;

		// Hostname filtern. Liste sollte nur ein Element haben; ansonsten Exception und Abbruch!
		List<List<String>> blocks = this.runningConfig.findParents(search_pattern_hostname);
		if (blocks.size() == 1) {
			this.name = extractGroup(pattern_hostname, blocks.get(0), 1);
		} else {
			throw new RuntimeException("No Hostname found in: " + runningConfig);
		}

		// Port Channel verarbeiten
		blocks = this.runningConfig.findParents(search_pattern_portchannel);
		blocks.removeIf(innerList -> innerList.size() < 2);
		for (List<String> block : blocks) {
			String name = extractGroup(pattern_int_name, block, 1);
			String desc = extractGroup(pattern_int_desc, block, 1);
			String int_type = extractGroup(pattern_int_vlan, block, 1);
			List<String> vlan_ids = extractGroups(pattern_int_vlan, block, 2);
			String vpc = extractGroup(pattern_int_vpc, block, 1);
			String peer_link = extractGroup(pattern_int_peerlink, block, 1);
			String ip = extractGroup(pattern_int_ip_sub, block, 1);
			String subnet = extractGroup(pattern_int_ip_sub, block, 2);

			// Annahme: VSL werden nur bei PortChannels definiert!
			String vsl = extractGroup(pattern_int_vsl, block, 1);

			// Neues Aggregator Interface erstellen
			Layer2Interface al2i = new Layer2Interface(name);
			al2i.runningConfig = block;
			al2i.description = desc;
			al2i.interfaceType = int_type;
			al2i.addVIDs(vlan_ids);
			al2i.vpc = vpc;
			al2i.peerLink = peer_link;
			al2i.vsl = vsl;

			// Assoziation NetworkComponent <-> Aggregator Interface
			this.aggregatorInterfaces.put(name, al2i);
			al2i.networkComponent = this;

			// Wenn eine IP-Adresse definiert wurde, neues Layer3Interface erstellen
			if (ip != null && subnet != null) {
				Layer3Interface l3i = new Layer3Interface(name);
				l3i.description = desc;
				l3i.ip = ip;
				l3i.subnet = subnet;

				// Assoziation NetworkComponent <-> Layer3Interface
				this.layer3Interfaces.put(name, l3i);
				l3i.networkComponent = this;

				// Assoziation Aggregator Interface <-> Layer3Interface
				al2i.layer3Interfaces.put(l3i.name, l3i);
				l3i.layer2Interfaces.put(al2i.name, al2i);
			}

			count_po++;
		}

		// Subinterfaces (Tagged Interfaces) verarbeiten, die auf Port Channels definiert sind
		blocks = this.runningConfig.findParents(search_pattern_po_sub);
		blocks.removeIf(innerList -> innerList.size() < 2);
		for (List<String> block : blocks) {
			String name = extractGroup(pattern_int_name, block, 1);
			String desc = extractGroup(pattern_int_desc, block, 1);
			String int_type = extractGroup(pattern_int_vlan_enc, block, 1);
			String vlan_ids = extractGroup(pattern_int_vlan_enc, block, 2);
			String ip = extractGroup(pattern_int_ip_sub, block, 1);
			String subnet = extractGroup(pattern_int_ip_sub, block, 2);

			// Vorhandenes Aggregator Interface raussuchen. Setzt voraus, dass Subinterfaces _nicht_ vor dem eigentlichen
			// Port Channel definiert werden! Sollte aber durch Running Config gegeben sein!
			Layer2Interface ul2i = this.aggregatorInterfaces.get(name.split(Pattern.quote("."))[0]);

			// Neues (Tagged) Interface erstellen
			Layer2Interface tl2i = new Layer2Interface(name);
			tl2i.runningConfig = block;
			tl2i.description = desc;
			tl2i.interfaceType = int_type;
			tl2i.addVIDs(vlan_ids);

			// Assoziation NetworkComponent <-> (Tagged) Interface
			this.layer2Interfaces.put(name, tl2i);
			tl2i.networkComponent = this;

			// Assoziation Aggregator Interface <-> (Tagged) Interface
			ul2i.taggedInterfaces.put(name, tl2i);
			tl2i.untaggedInterface = ul2i;

			// Wenn eine IP-Adresse definiert wurde, neues Layer3Interface erstellen
			if (ip != null && subnet != null) {
				Layer3Interface l3i = new Layer3Interface(name);
				l3i.description = desc;
				l3i.ip = ip;
				l3i.subnet = subnet;

				// Assoziation NetworkComponent <-> Layer3Interface
				this.layer3Interfaces.put(name, l3i);
				l3i.networkComponent = this;

				// Assoziation (Tagged) Interface <-> Layer3Interface
				tl2i.layer3Interfaces.put(l3i.name, l3i);
				l3i.layer2Interfaces.put(tl2i.name, tl2i);
			}

			count_po_subint++;
		}

		// Layer1Interfaces zusammen mit Layer2Interfaces und Layer3Interfaces verarbeiten
		blocks = this.runningConfig.findParents(search_pattern_interface);
		blocks.removeIf(innerList -> innerList.size() < 2);
		for (List<String> block : blocks) {
			String name = extractGroup(pattern_int_name, block, 1);
			String desc = extractGroup(pattern_int_desc, block, 1);
			String int_type = extractGroup(pattern_int_vlan, block, 1);
			List<String> vlan_ids = extractGroups(pattern_int_vlan, block, 2);
			String channelGroup = extractGroup(pattern_int_chgroup, block, 1);
			String ip = extractGroup(pattern_int_ip_sub, block, 1);
			String subnet = extractGroup(pattern_int_ip_sub, block, 2);

			// Neues Layer1Interface mit dazugehörigem Layer2Interface erstellen
			Layer1Interface l1i = new Layer1Interface(name);
			Layer2Interface sl2i = new Layer2Interface(name);
			sl2i.runningConfig = block;
			sl2i.description = desc;
			sl2i.interfaceType = int_type;
			sl2i.addVIDs(vlan_ids);

			// Wenn ein Port Channel gefunden wird, vorhandenes Aggregator Interface raussuchen und verlinken
			if (channelGroup != null) {
				// Setzt voraus, dass das Aggregation Interface _nicht_ vor dem Aggregator Interface definiert wurde. Sollte
				// aber in der Running Config gegeben sein.
				Layer2Interface al2i = this.aggregatorInterfaces.get("port-channel" + channelGroup);

				// Assoziation Layer1Interface <-> Aggregator Interface
				l1i.aggregatorInterfaces.put(al2i.name, al2i);
				al2i.layer1Interfaces.put(name, l1i);

				// Assoziation (Aggregation) Layer2Interface <-> Aggregator Interface
				sl2i.aggregator2Interface = al2i;
				al2i.aggregationInterfaces.put(name, sl2i);

				// Annahme: VSL werden nur bei PortChannels definiert!
				// Somit werden die VSL-Interfaces zwar mit ausgegeben, jedoch findet die Verarbeitung der Links anhand der
				// CDP-Informationen statt. Die Links müssen daher manuell nachgepflegt werden!
				if (al2i.vsl != null) {
					this.vss = true;
					al2i.description = "Switch Virtual Link";
					sl2i.description = "Switch Virtual Link";
					this.layer1InterfaceFilter.add(name);
				}
			}

			// Assoziation NetworkComponent <-> Layer1Interface
			this.layer1Interfaces.put(name, l1i);
			l1i.networkComponent = this;

			// Assoziation NetworkComponent <-> (Aggregation) Layer2Interface
			this.layer2Interfaces.put(name, sl2i);
			sl2i.networkComponent = this;

			// Assoziation Layer1Interface <-> (Aggregation) Layer2Interface
			l1i.layer2Interfaces.put(name, sl2i);
			sl2i.layer1Interfaces.put(name, l1i);

			// Wenn eine IP-Adresse definiert wurde, neues Layer3Interface erstellen. Schließt sich das mit dem Vorhandensein
			// eines Port Channels (Aggregator) aus?
			if (ip != null && subnet != null) {
				Layer3Interface l3i = new Layer3Interface(name);
				l3i.description = desc;
				l3i.ip = ip;
				l3i.subnet = subnet;

				// Assoziation NetworkComponent <-> Layer3Interface
				this.layer3Interfaces.put(name, l3i);
				l3i.networkComponent = this;

				// Assoziation (Aggregation) Layer2Interface <-> Layer3Interface
				sl2i.layer3Interfaces.put(l3i.name, l3i);
				l3i.layer2Interfaces.put(sl2i.name, sl2i);
			}

			count_int++;
		}

		// Subinterfaces (Tagged Interfaces) verarbeiten, die auf (einfachen) Layer2Interfaces definiert sind
		blocks = this.runningConfig.findParents(search_pattern_int_sub);
		blocks.removeIf(innerList -> innerList.size() < 2);
		for (List<String> block : blocks) {
			String name = extractGroup(pattern_int_name, block, 1);
			String desc = extractGroup(pattern_int_desc, block, 1);
			String int_type = extractGroup(pattern_int_vlan_enc, block, 1);
			String vlan_ids = extractGroup(pattern_int_vlan_enc, block, 2);
			String ip = extractGroup(pattern_int_ip_sub, block, 1);
			String subnet = extractGroup(pattern_int_ip_sub, block, 2);

			// Vorhandenes Layer2Interfaces raussuchen. Setzt voraus, dass Subinterfaces _nicht_ vor dem eigentlichen
			// Layer2Interface definiert werden! Sollte aber per Cisco Config gegeben sein.
			Layer2Interface ul2i = this.layer2Interfaces.get(name.split(Pattern.quote("."))[0]);

			// Neues Tagged Interface erstellen
			Layer2Interface tl2i = new Layer2Interface(name);
			tl2i.runningConfig = block;
			tl2i.description = desc;
			tl2i.interfaceType = int_type;
			tl2i.addVIDs(vlan_ids);

			// Assoziation NetworkComponent <-> Layer2Interface
			this.layer2Interfaces.put(name, tl2i);
			tl2i.networkComponent = this;

			// Assoziation Layer2Interface <-> Tagged Interface
			ul2i.taggedInterfaces.put(name, tl2i);
			tl2i.untaggedInterface = ul2i;

			// Wenn eine IP-Adresse definiert wurde, neues Layer3Interface erstellen
			if (ip != null && subnet != null) {
				Layer3Interface l3i = new Layer3Interface(name);
				l3i.description = desc;
				l3i.ip = ip;
				l3i.subnet = subnet;

				// Assoziation NetworkComponent <-> Layer3Interface
				this.layer3Interfaces.put(name, l3i);
				l3i.networkComponent = this;

				// Assoziation Tagged Interface <-> Layer3Interface
				tl2i.layer3Interfaces.put(l3i.name, l3i);
				l3i.layer2Interfaces.put(tl2i.name, tl2i);
			}

			count_int_subint++;
		}

		// Statistik ausgeben
		System.out.println("Hostname: " + this.name + (this.vss ? " - Virtual Switching System (VSS) detected!" : ""));
		System.out.println("> Interfaces found:\t" + count_int);
		System.out.println("> Subinterfaces found:\t" + count_int_subint);
		System.out.println("> Port-Channels found:\t" + count_po);
		System.out.println("> PO-Subinterf. found:\t" + count_po_subint);
		System.out.println();
	}

	/**
	 * Gibt die SOIL-Repräsentation der Netzkomponente als Liste von Strings zurück. Bei der Verarbeitung kommt es auf die
	 * richtige Reihenfolge der Ausgabe an, da die Assoziationen sonst nicht richtig gesetzt werden können. So muss
	 * beispielweise ein Aggregator Interface immer vor seinen Aggregation Interfaces definiert werden.
	 * 
	 * @return SOIL-Repräsentation als Liste von Strings
	 */
	public List<String> getClassSOIL() {
		List<String> result = new ArrayList<String>();
		List<Layer1Interface> layer1InterfacesFiltered = new ArrayList<>();
		Set<Layer2Interface> portChannelsFiltered = new HashSet<>();

		// Relevante Layer1Interfaces filtern. Leere Filterliste heißt "zeige alle Interfaces".
		if (this.layer1InterfaceFilter.isEmpty()) {
			layer1InterfacesFiltered = new ArrayList<>(this.layer1Interfaces.values());
		} else {
			for (String interfaceName : this.layer1InterfaceFilter) {
				Layer1Interface layer1Interface = this.layer1Interfaces.get(interfaceName);

				if (layer1Interface != null) {
					layer1InterfacesFiltered.add(layer1Interface);
				}
			}
		}

		// Relevante Port Channels (Aggregator) filtern. das Set verhindert doppelte Elemente!
		for (Layer1Interface layer1Interface : layer1InterfacesFiltered) {
			portChannelsFiltered.addAll(layer1Interface.aggregatorInterfaces.values());
		}

		int count_l1_int = 0;
		int count_l2_int = 0;
		int count_l2_agg_int = 0;
		int count_l2_tag_int = 0;
		int count_l3_int = 0;

		result.add("!nc := new NetworkComponent");
		result.add("!nc.name := '" + this.name + "'");
		result.add("");

		// Zunächst alle Aggregator Interfaces mit dazugehörigen Layer1Interface und Aggregation Interfaces verarbeiten
		for (Layer2Interface aggregatorInterface : portChannelsFiltered) {
			count_l2_agg_int++;
			result.addAll(aggregatorInterface.getSOIL());
			result.add("!insert (nc, al2i) into HasInterfaces");
			result.add("");

			for (Layer3Interface layer3Interface : aggregatorInterface.layer3Interfaces.values()) {
				count_l3_int++;
				result.addAll(layer3Interface.getSOIL());
				result.add("!insert (nc, l3i) into HasInterfaces");
				result.add("!insert (l3i, al2i) into DependsOnLayer2Interfaces");
				result.add("");
			}

			for (Layer2Interface taggedInterface : aggregatorInterface.taggedInterfaces.values()) {
				count_l2_tag_int++;
				result.addAll(taggedInterface.getSOIL());
				result.add("!insert (nc, tl2i) into HasInterfaces");
				result.add("!insert (tl2i, al2i) into DependsOnUntaggedDot1QInterface");
				result.add("");

				for (Layer3Interface layer3Interface : taggedInterface.layer3Interfaces.values()) {
					count_l3_int++;
					result.addAll(layer3Interface.getSOIL());
					result.add("!insert (nc, l3i) into HasInterfaces");
					result.add("!insert (l3i, tl2i) into DependsOnLayer2Interfaces");
					result.add("");
				}
			}

			for (Layer1Interface layer1Interface : aggregatorInterface.layer1Interfaces.values()) {
				count_l1_int++;
				layer1Interface.soilProcessed = true;
				result.addAll(layer1Interface.getSOIL());
				result.add("!insert (nc, l1i) into HasInterfaces");
				result.add("");

				for (Layer2Interface layer2Interface : layer1Interface.layer2Interfaces.values()) {
					count_l2_int++;
					result.addAll(layer2Interface.getSOIL());
					result.add("!insert (nc, sl2i) into HasInterfaces");
					result.add("!insert (sl2i, l1i) into DependsOnLayer1Interface");
					result.add("!insert (al2i, sl2i) into HasAggregationLayer2Interfaces");
					result.add("");

					// Layer3Interfaces müssen an dieser Stelle nicht beachtet werden, da es sich bei den Layer2Interfaces um
					// "UntaggedDot1QInterfaces"(siehe Topologiemodell) handeln muss, die keine L3I assoziiert haben dürften.
				}
			}
		}

		// Restliche Layer1Interfaces verarbeiten, die kein LAG haben und demnach noch nicht verarbeitet wurden
		for (Layer1Interface layer1Interface : layer1InterfacesFiltered) {
			if (!layer1Interface.soilProcessed) {
				count_l1_int++;
				result.addAll(layer1Interface.getSOIL());
				result.add("!insert (nc, l1i) into HasInterfaces");
				result.add("");

				for (Layer2Interface layer2Interface : layer1Interface.layer2Interfaces.values()) {
					count_l2_int++;
					result.addAll(layer2Interface.getSOIL());
					result.add("!insert (nc, sl2i) into HasInterfaces");
					result.add("!insert (sl2i, l1i) into DependsOnLayer1Interface");
					result.add("");

					for (Layer3Interface layer3Interface : layer2Interface.layer3Interfaces.values()) {
						count_l3_int++;
						result.addAll(layer3Interface.getSOIL());
						result.add("!insert (nc, l3i) into HasInterfaces");
						result.add("!insert (l3i, sl2i) into DependsOnLayer2Interfaces");
						result.add("");
					}

					for (Layer2Interface taggedInterface : layer2Interface.taggedInterfaces.values()) {
						count_l2_tag_int++;
						result.addAll(taggedInterface.getSOIL());
						result.add("!insert (nc, tl2i) into HasInterfaces");
						result.add("!insert (tl2i, sl2i) into DependsOnUntaggedDot1QInterface");
						result.add("");

						for (Layer3Interface layer3Interface : taggedInterface.layer3Interfaces.values()) {
							count_l3_int++;
							result.addAll(layer3Interface.getSOIL());
							result.add("!insert (nc, l3i) into HasInterfaces");
							result.add("!insert (l3i, tl2i) into DependsOnLayer2Interfaces");
							result.add("");
						}
					}
				}
			}

			// Verarbeitungsflag wieder zurücksetzen. Spart eine Schleife am Ende!
			layer1Interface.soilProcessed = false;
		}

		System.out.println("NetworkComponent: " + this.name + (this.vss ? " (VSS!)" : ""));
		System.out.println("Layer1Interfaces created: " + count_l1_int);
		System.out.println("Layer2Interfaces created: " + count_l2_int);
		System.out.println("Layer2Interfaces (Agg.) created: " + count_l2_agg_int);
		System.out.println("Layer2Interfaces (Tag.) created: " + count_l2_tag_int);
		System.out.println("Layer3Interfaces created: " + count_l3_int);
		System.out.println();

		return result;
	}
}
