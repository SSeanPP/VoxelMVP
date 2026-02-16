package resourceLoader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class ResourceLoader {
	
	public ResourceLoader() {
		
	}
	
	public static String loadResourceAsString(String fileName) throws IOException {
		
		URL url = ResourceLoader.class.getClassLoader().getResource("");
		System.out.println("Classpath root is: " + url);
		InputStream is = ResourceLoader.class.getClassLoader().getResourceAsStream(fileName);
		//System.out.println("Classpath root is: " + is);
		
	    if (is == null) {
	        throw new FileNotFoundException("Resource not found: " + fileName);
	    }

	    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
	    StringBuilder sb = new StringBuilder();
	    String line;

	    try {
	        while ((line = reader.readLine()) != null) {
	            sb.append(line).append("\n");
	        }
	    } finally {
	        // Essential for Java 1.6 to prevent memory leaks
	        reader.close();
	    }

	    return sb.toString();
	}
}
