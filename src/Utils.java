import java.util.HashMap;
import java.util.Map;

final class Utils {
	private Utils() {}; // can't instantiate this static class

	// Chew on the provided arguments and build a map
	static Map<String, String> parseCmdlineFlags(String argv[]) {
		Map<String, String> flags = new HashMap<String, String>();
		for (String flag : argv) {
			if (flag.startsWith("--")) {	
				String[] parts = flag.split("=");
				if (parts.length == 2) {
					flags.put(parts[0], parts[1]);
				}
			}
		}
		return flags;
	}
}

