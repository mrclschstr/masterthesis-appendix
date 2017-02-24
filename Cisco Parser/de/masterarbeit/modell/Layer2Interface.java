package de.masterarbeit.modell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Einfache Repräsentation eines Interfaces der zweiten (OSI-)Schicht. In diesem Falle ein Ethernet Interface ohne speziellen
 * Typ (Access, Trunk, Untagged, Tagged). Der Typ wird bei der SOIL-Ausgabe aus den Attributen abgeleitet.
 * 
 * TODO Vererbung mit gemeinsamer abstrakter Klassen "Interface" wäre sinnvoll, ist aber nicht notwending.
 * 
 * @author Marcel Schuster
 *
 */
public class Layer2Interface {
	public String name;
	public String description;
	public String interfaceType;
	public String vpc;
	public String peerLink;
	public String vsl;
	protected List<String> vlanIDs = new ArrayList<>();

	// Speicher den aktuellen Block (siehe JCiscoConfParse) des Interfaces zu Analysezwecken
	public List<String> runningConfig = new ArrayList<>();

	// SOIL-Ausgabe bereits bearbeitet?
	public boolean soilProcessed = false;

	// Wurde der Link (Verbindung) bereits verarbeitet?
	public boolean linkProcessed = false;

	// Wurde die SOIL-Ausgabe des Links bereits verarbeitet?
	public boolean linkSoilProcessed = false;

	public NetworkComponent networkComponent;

	// Layer 1 Assoziationen
	public Map<String, Layer1Interface> layer1Interfaces = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	// Layer 2 Assoziationen
	public Layer2Interface mergedInterface;
	public Map<String, Layer2Interface> layer2LinksTo = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	public Layer2Interface untaggedInterface;
	public Map<String, Layer2Interface> taggedInterfaces = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	public Layer2Interface aggregator2Interface;
	public Map<String, Layer2Interface> aggregationInterfaces = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	// Layer 3 Assoziationen
	public Map<String, Layer3Interface> layer3Interfaces = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	/**
	 * Konstruktur zur Initialisierung.
	 * 
	 * @param name
	 *            Name des Interfaces
	 */
	public Layer2Interface(String name) {
		this.name = name;
	}

	/**
	 * Gibt die SOIL-Repräsentation des Interfaces als Liste von Strings zurück. Besonderheit hierbei ist, dass der Typ des
	 * Interfaces aus den gesetzten Parametern (hauptsächlich "interfaceType") abgeleitet wird.
	 * 
	 * @return SOIL-Repräsentation als Liste von Strings
	 */
	public List<String> getSOIL() {
		List<String> result = new ArrayList<>();
		String var = !this.aggregationInterfaces.isEmpty() ? "al2i" : "sl2i";

		if (this.mergedInterface != null && this.mergedInterface.soilProcessed) {
			// Wenn Interface verschmolzen ist und Partner bereits ausgegeben wurde, wird dieses per OCL rausgesucht.
			result.add("!" + var + " := NetworkComponent.allInstances()->any(p | p.name = '"
					+ this.mergedInterface.networkComponent.name + "').getLayer2Interfaces()->any(i | i.name='"
					+ this.mergedInterface.name + "')");
		} else if (interfaceType == null) {
			result.add("!" + var + " := new " + (this.taggedInterfaces() ? "UntaggedDot1QInterface" : "AccessInterface"));
			result.add("!" + var + ".name := '" + this.name + "'");
			result.add("!" + var + ".description := '" + this.desc2String() + "'");
		} else if (interfaceType.equalsIgnoreCase("access")) {
			// Annahme: frameType = #admitAll
			result.add("!" + var + " := new AccessInterface");
			result.add("!" + var + ".name := '" + this.name + "'");
			result.add("!" + var + ".description := '" + this.desc2String() + "'");
			result.add("!" + var + ".PVID = " + this.vids2String());
		} else if (interfaceType.equalsIgnoreCase("trunk")) {
			// Annahme: frameType = #admitAll
			result.add("!" + var + " := new TrunkInterface");
			result.add("!" + var + ".name := '" + this.name + "'");
			result.add("!" + var + ".description := '" + this.desc2String() + "'");
			result.add("!" + var + ".VID := Set{" + this.vids2String() + "}");
		} else if (interfaceType.equalsIgnoreCase("encapsulation")) {
			// Annahme: TaggedInterface ist nie aggregiert
			result.add("!tl2i := new TaggedDot1QInterface");
			result.add("!tl2i.name := '" + this.name + "'");
			result.add("!tl2i.description := '" + this.desc2String() + "'");
			result.add("!tl2i.VID := Set{" + this.vids2String() + "}");
			result.add("!tl2i.PVID := -1");
			result.add("!tl2i.frameType := #admitTagged");
		} else {
			// Panik?
		}

		this.soilProcessed = true;

		// Zu Analysezwecken kann an dieser Stelle auch "runningConfig" zurückgegeben werden!
		// return this.runningConfig;
		return result;
	}

	/**
	 * Fügt VLAN-IDs zum Interface hinzu. Dabei sind Angaben der Art "5", "5-10" oder auch "5,10,20-100" möglich, wie sie
	 * auch in Cisco Konfigurationsdateien vorkommen können. Zahlenbereiche werden aufgelöst; also "5-8" wird zu "5,6,7,8,".
	 * 
	 * @param vid
	 *            VLAN-ID String, der zum Interface hinzugefügt werden soll
	 */
	public void addVIDs(String vid) {
		if (vid != null) {
			String[] vidsArray = vid.split(Pattern.quote(","));

			for (String elem : vidsArray) {
				if (Pattern.matches("^[\\d]+$", elem)) {
					this.vlanIDs.add(elem);
				} else if (Pattern.matches("^[\\d]+-[\\d]+$", elem)) {
					String[] range = elem.split(Pattern.quote("-"));
					Integer from = Integer.parseInt(range[0]);
					Integer to = Integer.parseInt(range[1]);

					for (int i = from; i <= to; i++) {
						this.vlanIDs.add(Integer.toString(i));
					}
				} else {
					// Panik?
				}
			}
		}
	}

	/**
	 * Fügt mehrere VLAN-IDs zum aktuellen Interface hinzu und benutzt dabei die Operation addVIDs(). Kommt meistens bei
	 * Trunks vor, bei denen mehrzeilig VLAN-ID hinzugefügt (bzw. erlaubt) werden können.
	 * 
	 * @param vids
	 *            Liste von VLAN-IDs, die hinzugefügt werden sollen
	 */
	public void addVIDs(List<String> vids) {
		for (String vid : vids) {
			this.addVIDs(vid);
		}
	}

	/**
	 * Prüfung, ob sich die VLAN-IDs der zwei Interfaces überschneiden. Falls ja, können sich die Interfaces gegenseitig
	 * Frames mit den entsprechenden (überschneidenden) VLAN-IDs schicken.
	 * 
	 * Hinweis: Stellt nicht sicher, dass die VLAN Konfiguration beider Interfaces gleich bzw. konsistent ist!
	 * 
	 * @param layer2Interface
	 *            Interface, für welches die Überschneidung geprüft werden soll
	 * @return true, wenn sich die VLAN-IDs der Interfaces überschneiden
	 */
	public boolean vlanIntersection(Layer2Interface layer2Interface) {
		List<String> tmpVlanIDs = new ArrayList<>(this.vlanIDs);
		tmpVlanIDs.retainAll(layer2Interface.vlanIDs);
		return !tmpVlanIDs.isEmpty();
	}

	/**
	 * Prüft, ob das aktuelle oder das AggregatorInterface TaggedInterfaces (Subinterfaces) definiert haben.
	 * 
	 * @return true, sofern TaggedInterfaces "in der Nähe" sind
	 */
	private boolean taggedInterfaces() {
		return !this.taggedInterfaces.isEmpty()
				|| (this.aggregator2Interface != null && !this.aggregator2Interface.taggedInterfaces.isEmpty());
	}

	/**
	 * Gibt die Liste der VLAN-IDs des Interfaces getrennt nach Komma zurück.
	 * 
	 * @return VLAN-IDs als String
	 */
	private String vids2String() {
		return String.join(",", this.vlanIDs);
	}

	/**
	 * Gibt die Beschreibung des Interfaces zurück und verhindert die Rückgabe von null.
	 * 
	 * @return Beschreibgung des Interfaces
	 */
	private String desc2String() {
		return this.description == null ? "" : this.description;
	}
}
