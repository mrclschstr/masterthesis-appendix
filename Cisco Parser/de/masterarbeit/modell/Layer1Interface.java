package de.masterarbeit.modell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Einfache Repräsentation eines Interfaces der ersten (OSI-)Schicht. In diesem Falle ein physisches Interface.
 * 
 * TODO Vererbung mit gemeinsamer abstrakter Klassen "Interface" wäre sinnvoll, ist aber nicht notwending.
 * 
 * @author Marcel Schuster
 *
 */
public class Layer1Interface {
	public String name;

	// SOIL-Ausgabe bereits bearbeitet?
	public boolean soilProcessed = false;

	// Wurde der Link (Verbindung) bereits verarbeitet?
	public boolean linkProcessed = false;

	// Wurde die SOIL-Ausgabe des Links bereits verarbeitet?
	public boolean linkSoilProcessed = false;

	// Wurde das Interface bereits verschmolzen (MC-LAG)?
	public boolean mergeProcessed = false;

	// Keine Maps nötig, da nur ein Element möglich!
	public NetworkComponent networkComponent;
	public Layer1Interface layer1LinkTo;

	// Verschiedene Listen sind zwar umständlich, aber spart unnötiges Filtern!
	// Bei den layer2Interfaces kann es sich auch um Aggregation Interfaces handeln (Rollenname)!
	public Map<String, Layer2Interface> layer2Interfaces = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	public Map<String, Layer2Interface> aggregatorInterfaces = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	/**
	 * Konstruktor zur Initialisierung
	 * 
	 * @param name
	 *            Name des Interfaces
	 */
	public Layer1Interface(String name) {
		this.name = name;
	}

	/**
	 * Gibt die SOIL-Repräsentation des Interfaces als Liste von Strings zurück.
	 * 
	 * @return SOIL-Repräsentation als Liste von Strings
	 */
	public List<String> getSOIL() {
		List<String> result = new ArrayList<>();

		result.add("!l1i := new Layer1Interface");
		result.add("!l1i.name := '" + this.name + "'");

		return result;
	}

	/**
	 * Liefert alle Layer2Interfaces zurück, die auf diesem Layer1Interface definiert sind. Die logisch "höchsten" Interfaces
	 * stehen dabei vorne in der Liste: Tagged -> Aggregator -> Aggregation/Simple. Nutzt streng genommen die Funktionsweise
	 * der addAll() Operation aus!
	 * 
	 * @return Liste aller assoziierten Layer2Interfaces
	 */
	public List<Layer2Interface> getAllLayer2Interfaces() {
		List<Layer2Interface> result = new ArrayList<>();

		// TaggedInterfaces auf Aggregator Interfaces
		for (Layer2Interface tagged : this.aggregatorInterfaces.values()) {
			result.addAll(tagged.taggedInterfaces.values());
		}

		// TaggedInterfaces von (Aggregation) Interfaces
		for (Layer2Interface tagged : this.layer2Interfaces.values()) {
			result.addAll(tagged.taggedInterfaces.values());
		}

		// Aggregator Interfaces
		result.addAll(this.aggregatorInterfaces.values());

		// (Aggregation) Interfaces
		result.addAll(this.layer2Interfaces.values());

		return result;
	}
}
