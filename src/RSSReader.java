import org.jsoup.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class RSSReader {

    static int MAX_ITEMS=5;
    static int Counter =0;
    static String[][] Info = new String[1010][3];

    public static void main(String[] args) {
        startUp();
        String action;
        while (true) {
            System.out.println("Type a valid number for your desired action:");
            System.out.println("[1] Show updates");
            System.out.println("[2] Add URL");
            System.out.println("[3] Remove URL");
            System.out.println("[4] Exit");
            action = new Scanner(System.in).nextLine();
            if (action.equals("1") || action.equals("2") || action.equals("3") || action.equals("4")) {
                switch(action) {
                    case "1":
                        showUpdates();
                        break;
                    case "2":
                        addUrl();
                        break;
                    case "3":
                        removeUrl();
                        break;
                    case "4":
                        shutDown();
                        return;
                }
            } else {
                System.out.println("invalid request!\n");
            }
        }
    }

    private static void startUp() {
        System.out.println("Welcome to RSS Reader!");
        File data = new File("data.txt");
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(data))) {
            if (data.createNewFile()) {
                System.out.println("Data file created.");
            } else {
                String temp;
                while ((temp = bufferedReader.readLine()) != null) {
                    Info[Counter++] = temp.split(";");
                    System.out.println(Info[Counter - 1][2]);
                }
            }
        } catch (IOException e) {
            System.out.println("Error in reading data file: " + e.getMessage());
        }
    }

    private static void showUpdates() {
        while (true) {
            System.out.println("Show updates for:");
            System.out.println("[0] All websites");
            for (int i = 0; i < Counter; i++) {
                System.out.println("[" + (i + 1) + "] " + Info[i][0]);
            }
            System.out.println("Enter -1 to return.");
            String temp = new Scanner(System.in).nextLine();
            try {
                int req = Integer.parseInt(temp);
                if (req == -1) {
                    return;
                } else if (req < -1 || req > Counter) {
                    System.out.println("Invalid request!");
                } else if (req == 0) {
                    for (int i = 0; i < Counter; i++) {
                        System.out.println(Info[i][0]);
                        retrieveRssContent(Info[i][2]);
                        System.out.println();
                    }
                } else {
                    System.out.println(Info[req - 1][0]);
                    retrieveRssContent(Info[req - 1][2]);
                    System.out.println();
                }
                return;
            } catch (Exception e) {
                System.out.println("Error in showing updates: " + e.getMessage());
            }
        }
    }

    private static void addUrl() {
        System.out.println("Please enter website URL to add:");
        String url = new Scanner(System.in).nextLine();
        try {
            String pageSource = fetchPageSource(url);
            String rssUrl = extractRssUrl(url);
            String pageTitle = extractPageTitle(pageSource);
            boolean exists = false;
            for (int i = 0; i < Counter; i++) {
                if (Info[i][2].equals(rssUrl)) {
                    exists = true;
                    break;
                }
            }
            if (exists) {
                System.out.println(url + " already exists.");
            } else {
                Info[Counter][0] = pageTitle;
                Info[Counter][1] = url;
                Info[Counter][2] = rssUrl;
                Counter++;
                System.out.println("Added " + url + " successfully.");
                System.out.println();
            }
        } catch (Exception e) {
            System.out.println("Error in adding URL: " + e.getMessage());
        }
    }

    private static void removeUrl() {
        System.out.println("Please enter website URL to remove:");
        String url = new Scanner(System.in).nextLine();
        boolean found = false;
        for (int i = 0; i < Counter; i++) {
            if (Info[i][1].equals(url)) {
                for (int j = i; j < Counter - 1; j++) {
                    Info[j] = Info[j + 1];
                }
                Counter--;
                found = true;
                System.out.println("Removed " + url + " successfully.");
                System.out.println();
                break;
            }
        }
        if (!found) {
            System.out.println("Couldn't find " + url + ".");
            System.out.println();
        }
    }

    private static void shutDown() {
        System.out.println("Shutting down the RSS Reader...");
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("data.txt"))) {
            for (int i = 0; i < Counter; i++) {
                bufferedWriter.write(String.join(";", Info[i]) + "\n");
            }
        } catch (IOException e) {
            System.out.println("Error in writing data file: " + e.getMessage());
        }
    }

    public static void retrieveRssContent(String rssUrl) {
        try {
            String rssXml = fetchPageSource(rssUrl);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            StringBuilder xmlStringBuilder = new StringBuilder();
            xmlStringBuilder.append(rssXml);
            ByteArrayInputStream input = new ByteArrayInputStream(xmlStringBuilder.toString().getBytes("UTF-8"));
            org.w3c.dom.Document doc = documentBuilder.parse(input);
            NodeList itemNodes = doc.getElementsByTagName("item");
            for (int i = 0; i < MAX_ITEMS; ++i) {
                Node itemNode = itemNodes.item(i);
                if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) itemNode;
                    System.out.println("Title: " + element.getElementsByTagName("title").item(0).getTextContent());
                    System.out.println("Link: " + element.getElementsByTagName("link").item(0).getTextContent());
                    System.out.println("Description: " + element.getElementsByTagName("description").item(0).getTextContent());
                }
            }
        } catch (Exception e) {
            System.out.println("Error in retrieving RSS content for " + rssUrl + ": " + e.getMessage());
        }
    }

    public static String extractPageTitle(String html) {
        try {
            org.jsoup.nodes.Document doc = Jsoup.parse(html);
            return doc.select("title").first().text();
        } catch (Exception e) {
            return "Error: no title tag found in page source!";
        }
    }

    public static String extractRssUrl(String url) throws IOException {
        org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
        return doc.select("[type='application/rss+xml']").attr("abs:href");
    }

    public static String fetchPageSource(String urlString) throws Exception {
        URI uri = new URI(urlString);
        URL url = uri.toURL();
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");
        return toString(urlConnection.getInputStream());
    }

    private static String toString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream , "UTF-8"));
        String inputLine;
        StringBuilder stringBuilder = new StringBuilder();
        while ((inputLine = bufferedReader.readLine()) != null) {
            stringBuilder.append(inputLine);
        }
        return stringBuilder.toString();
    }
}