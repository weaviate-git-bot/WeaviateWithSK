package soham.weaviate;

import java.io.IOException;
import java.util.Properties;

public class ReadConfig {

    public static String readProperty(String key) throws IOException {
        Properties properties = new Properties();
        java.net.URL url = ClassLoader.getSystemResource("conf.properties");
        properties.load(url.openStream());
        return properties.getProperty(key);
    }
}
