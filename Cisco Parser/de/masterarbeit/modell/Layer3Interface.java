package de.masterarbeit.modell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Einfache Repräsentation eines Interfaces der dritten (OSI-)Schicht. In diesem Falle ein IPv4-Interface.
 * 
 * TODO Vererbung mit gemeinsamer abstrakter Klassen "Interface" wäre sinnvoll, ist aber nicht notwending.
 * 
 * @author Marcel Schuster
 *
 */
public class Layer3Interface {
	public String name;
	public String description;
	public String ip;
	public String subnet;

	public NetworkComponent networkComponent;
	public Map<String, Layer2Interface> layer2Interfaces = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	/**
	 * Konstruktur zur Initialisierung.
	 * 
	 * @param name
	 *            Name des Interfaces
	 */
	public Layer3Interface(String name) {
		this.name = name;
	}

	/**
	 * Gibt die SOIL-Repräsentation des Interfaces als Liste von Strings zurück.
	 * 
	 * @return SOIL-Repräsentation als Liste von Strings
	 */
	public List<String> getSOIL() {
		List<String> result = new ArrayList<>();

		result.add("!l3i := new IPv4Interface");
		result.add("!l3i.name := '" + this.name + "'");
		result.add("!l3i.description := '" + this.description + "'");
		result.add("!l3i.IP := Sequence{" + String.join(",", this.ip.split(Pattern.quote("."))) + "}");
		result.add("!l3i.subnet := Sequence{" + String.join(",", this.getSubnetmask().split(Pattern.quote("."))) + "}");

		return result;
	}

	/**
	 * Gibt die definierte Subnetzmaske als String zurück. Die Operation übersetzt dabei die CIDR-Suffix Notation in die
	 * Dotted Decimal Notation, sofern notwendig. Siehe auch: https://de.wikipedia.org/wiki/Classless_Inter-Domain_Routing.
	 * 
	 * http://www.willa.me/2013/11/the-six-most-common-species-of-code.html -> Hackathon!
	 * 
	 * @return Definierte Subnetzmaske des Interfaces
	 */
	private String getSubnetmask() {
		if (Pattern.matches("^(?:[\\d]{1,3}\\.){3}[\\d]{1,3}$", this.subnet)) {
			return this.subnet;
		} else if (Pattern.matches("^/[\\d]+$", this.subnet)) {
			switch (this.subnet) {
			case "/24":
				return "255.255.255.0";
			case "/30":
				return "255.255.255.252";
			case "/32":
				return "255.255.255.255";
			default:
				return "<NOT_FOUND>";
			}
		}

		// Panik?
		return null;
	}
}
