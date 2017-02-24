package de.masterarbeit.toolbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kleine Toolbox Klasse, zur Kapselung von gemeinsam genutzten Operationen.
 * 
 * @author Marcel Schuster
 *
 */
public class Toolbox {
	/**
	 * Wendet das Regex Pattern auf jede Line eines Blocks an und liefert capturing group zurück, sofern ein Treffer gefunden
	 * wird. Wichtig: Es wird nur der erste Treffer beachtet!
	 * 
	 * @param pattern
	 *            Regex Pattern, nach welchem gesucht werden soll
	 * @param block
	 *            Ein Block einer Cisco Konfigurationsdatei
	 * @param group
	 *            Nummer der capturing group (siehe "pattern"), die bei einem match zurückgegeben werden soll
	 * @return Gefundene capturing group oder null, sofern kein match gefunden wurde
	 */
	public static String extractGroup(String pattern, List<String> block, int group) {
		for (String line : block) {
			Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(line);
			if (matcher.find()) {
				return matcher.toMatchResult().group(group).trim();
			}
		}

		return null;
	}

	/**
	 * Wendet das Regex Pattern auf jede Line eines Blocks an und liefert Gruppe zurück, sofern ein Treffer gefunden wird.
	 * Wichtig: Es werde alle Treffer beachtet!
	 * 
	 * @param pattern
	 *            Regex Pattern, nach welchem gesucht werden soll
	 * @param block
	 *            Ein Block einer Cisco Konfigurationsdatei
	 * @param group
	 *            Nummer der capturing group (siehe "pattern"), die bei einem match zurückgegeben werden soll
	 * @return Gefundene capturing group oder null, sofern kein match gefunden wurde
	 */
	public static List<String> extractGroups(String pattern, List<String> block, int group) {
		List<String> result = new ArrayList<>();

		for (String line : block) {
			Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(line);
			if (matcher.find()) {
				result.add(matcher.toMatchResult().group(group).trim());
			}
		}

		return result;
	}

	/**
	 * Parst eine Liste von Blöcken der CDP-Ausgabe, die vorher am besten mit der Operation findNeighbors() gefiltert wurde.
	 * Die Operation gibt eine Map zurück, die jedem lokalen Interface ein dazugehöriges Interface einer benachbarten
	 * Netzkomponente zuordnet.
	 * 
	 * @param blocks
	 *            Die gefilterten CDP-Blöcke, die geparst werden sollen
	 * @return Map, die jedem lokalen Interface ein Remote Interface zuordnet
	 */
	public static Map<String, String> getInterfaceNamesToNeighbors(List<List<String>> blocks) {
		Map<String, String> result = new HashMap<>();

		// Sichtbares Zeichen: \p{Graph} = [\p{Alnum}\p{Punct}]
		// Interfaces enthalten manchmal Leerzeichen: "Ten1/1/1" oder "Ten 1/1/1"
		for (List<String> block : blocks) {
			String pattern = null;

			// Je nachdem, ob der Block ein- oder zweizeilig ist, muss ein anderes Regex Pattern verwendet werden
			if (block.size() == 1) {
				pattern = "^(?:[\\p{Graph}]+)[\\s]+([\\p{Graph} ]+)[\\s]+(?:[\\d]+)[\\s]+(?:[BCDHIMPRSTVrs ]+)[\\s]+(?:[\\p{Graph}]+)[\\s]+([\\p{Graph} ]+)(?:.*)$";
			} else if (block.size() == 2) {
				pattern = "^[\\s]+([\\p{Graph} ]+)[\\s]+(?:[\\d]+)[\\s]+(?:[BCDHIMPRSTVrs ]+)[\\s]+(?:[\\p{Graph}]+)[\\s]+([\\p{Graph} ]+)(?:.*)$";
			} else {
				// Panik?
			}

			String localInterface = longInterfaceName(extractGroup(pattern, block, 1));
			String remoteInterface = longInterfaceName(extractGroup(pattern, block, 2));
			result.put(localInterface, remoteInterface);
		}

		return result;
	}

	/**
	 * Übersetzt das Kürzel des Interfacenamens aus der CDP-Ausgabe in den vollständigen Namen der Konfigurationsdatei.
	 * Beispiel: Gig0/1 -> GigabitEthernet0/1
	 * 
	 * http://www.willa.me/2013/11/the-six-most-common-species-of-code.html -> Hackathon!
	 * 
	 * @param shortName
	 *            Kürzel, welches übersetzt werden soll
	 * @return Vollständiger Name des Interfaces
	 */
	private static String longInterfaceName(String shortName) {
		// Interfaces enthalten manchmal Leerzeichen: "Ten1/1/1" oder "Ten 1/1/1"
		Matcher matcher = Pattern.compile("^(Eth|Gig|Ten)[\\s]?([\\d/]+)$", Pattern.CASE_INSENSITIVE).matcher(shortName);

		// Kürzel übersetzen, sofern die Eingabe dem korrekten Format entspricht
		if (matcher != null && matcher.find()) {
			String type = matcher.toMatchResult().group(1);
			String port = matcher.toMatchResult().group(2);

			switch (type) {
			case "Eth":
				return "Ethernet" + port;
			case "Gig":
				return "GigabitEthernet" + port;
			case "Ten":
				return "TenGigabitEthernet" + port;
			case "mgmt0":
				return "ManagementInterface";
			default:
				return "<NOT_FOUND>";
			}
		}

		// Gibt das Kürzel zurück, sofern es nicht dem korrekten Format entspricht
		return shortName;
	}
}
