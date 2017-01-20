
/**
 * Created by dubze on 1/12/2017.
 */
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.AWTException;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class CourseCon {
    private static String OS;
    private static String PATH_DELIM;
    private static String USERNAME;

    private static final String ourvle = "http://ourvle.mona.uwi.edu/";

    private static String name, userName, userPass, token, subPath;
    private static int userId;
    private static int count = 0;
    private static ArrayList<String> newDownloads = new ArrayList<String>();
    private static File bat, subJson;
    private static CourseCon cc = new CourseCon();
    private static TrayIcon trayIcon;

    public static void main(String[] args) throws Exception {
        OS = System.getProperty("os.name").toLowerCase();
        USERNAME = System.getProperty("user.name");

        if (OS.equals("linux")) {
            PATH_DELIM = "/";
        } else {
            PATH_DELIM = "\\";
            try {
                trayView();
            } catch (NullPointerException e) {
                /* empty */
            }

            createBat();
        }

        config();

        String siteInfo = cc.sendGet(ourvle + "webservice/rest/server.php?wstoken=" + token + "&wsfunction=core_webservice_get_site_info&moodlewsrestformat=json");

        while (siteInfo.equals("Fail")) {
            JOptionPane.showMessageDialog(new JFrame(), "No Internet Connection!\nConnect to a valid network then click OK.");
            siteInfo = cc.sendGet(ourvle + "webservice/rest/server.php?wstoken=" + token + "&wsfunction=core_webservice_get_site_info&moodlewsrestformat=json");
        }
        
        final JSONObject o = getJsonObj(siteInfo);
        userId = parseID(o);

        final String usersCourses = cc.sendGet(ourvle + "webservice/rest/server.php?wsfunction=core_enrol_get_users_courses&moodlewsrestformat=json&userid=" + userId + "&wstoken=" + token);

        final JSONArray subs = getJsonArray(usersCourses);

        while (true) {
            parseSubjects(subs);
            String s = "";

            if (count > 0) {
                for (int f = 0; f < newDownloads.size(); f++) {
                    s += newDownloads.get(f) + "....";
                }

                if (!OS.equals("linux")) {
                    String m = "New files have been added to:" + s;

                    trayIcon.displayMessage("Notice", m, TrayIcon.MessageType.INFO);
                }

                count = 0;
                newDownloads.removeAll(newDownloads);
            }

            System.out.println("Done");

            try {
                Thread.sleep(3600000);
            } catch (InterruptedException e) {
                JOptionPane.showMessageDialog(new JFrame(), e.getMessage());
            }
        }
    }

    // HTTP GET request
    private static String sendGet(String url) {
        try {
            final URL obj = new URL(url);
            final HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            final BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));

            final StringBuilder response = new StringBuilder();

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString();
        } catch (Exception e) {
            return e.toString();
        }
    }

    // HTTP POST request
    private static String sendPost(String url, String body) throws Exception {
        try {
            final URL obj = new URL(url);
            final HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // Send post request
            con.setDoOutput(true);
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            final DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(body);
            wr.flush();
            wr.close();

            final BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));

            final StringBuilder response = new StringBuilder(128);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString();
        } catch (Exception e) {
            return e.toString();
        }
    }

    private static JSONObject getJsonObj(String str) {
        final JSONObject ob = new JSONObject(str);
        return ob;
    }

    private static JSONArray getJsonArray(String str) {
        final JSONArray ob = new JSONArray(str);
        return ob;
    }

    private static String getToken(JSONObject ob) {
        final String result = ob.getString("token");
        return result;
    }

    private static int parseID(JSONObject info) {
        final int result = info.getInt("userid");
        return result;
    }

    private static String parseUserName(JSONObject info) {
        final String result = info.getString("username");
        return result;
    }

    private static void parseSubjects(JSONArray subs) throws Exception {
        for (int i = 0; i < subs.length(); i++) {
            name = (subs.getJSONObject(i)).getString("shortname");
            String path = subPath + name;
            int courseId = (subs.getJSONObject(i)).getInt("id");
            File file = new File(path);
            file.mkdir();
            subFld(name, courseId);
        }
    }

    private static void subFld(String fldName, int subId) throws Exception {
        String subData;
        
        try {
            subData = cc.sendGet(ourvle + "webservice/rest/server.php?courseid=" + subId + "&moodlewsrestformat=json&wstoken=" + token + "&wsfunction=core_course_get_contents");
        } catch (JSONException e) {
            JOptionPane.showMessageDialog(new JFrame(), e.getMessage());
            subData = null;
        }

        final JSONArray subArray = getJsonArray(subData);
        for (int z = 0; z < subArray.length(); z++) {
            JSONObject subObj = subArray.getJSONObject(z);
            JSONArray mods = subObj.getJSONArray("modules");
            
            for (int m = 0; m < mods.length(); m++) {
                JSONObject modJ = mods.getJSONObject(m);

                if (!(modJ.isNull("contents"))) {
                    JSONArray cont = modJ.getJSONArray("contents");

                    for (int c = 0; c < cont.length(); c++) {
                        JSONObject subCont = cont.getJSONObject(c);
                        fileDownload(fldName, subCont);
                    }
                }
            }
        }
    }

    private static void fileDownload(String fld, JSONObject jsonFiles) throws IOException {
        if (!jsonFiles.isNull("filename")) {
            final String u = jsonFiles.getString("fileurl") + "&token=" + token;

            try {
                final URL website = new URL(u);
                final ReadableByteChannel rbc = Channels.newChannel(website.openStream());

                final String filePath = subPath + fld + PATH_DELIM + jsonFiles.getString("filename");
                final String downPath = filePath.replaceAll("\\s+", "");

                final File down = new File(downPath);
                if (!(down.exists())) {
                    final FileOutputStream fos = new FileOutputStream(downPath);

                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                    count += 1;
                    if (!(newDownloads.contains(fld))) {
                        newDownloads.add(fld);
                    }
                }

            } catch (FileNotFoundException e) {
                /* empty */
            }
        }
    }

    private static void config() throws Exception {
        String result = "";

        String confFilePath;
        if (OS.equals("linux")) {
            confFilePath = "/home/" + USERNAME + "/.CourseCon/config.json";
        } else {
            confFilePath = "C:\\Users\\" + USERNAME + "\\Documents\\CourseConFiles\\config.json";
        }

        subJson = new File(confFilePath);
        if (subJson.exists()) {
            try {
                final BufferedReader br = new BufferedReader(new FileReader(subJson));
                final StringBuilder sb = new StringBuilder((int) subJson.length());
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                result = sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }

            final JSONObject jobj = new JSONObject(result);

            userName = jobj.getString("username");
            userPass = jobj.getString("password");
            subPath = jobj.getString("path") + USERNAME + jobj.getString("path2");
            if (jobj.isNull("token")) {
                final String tokenUrl = ourvle + "/login/token.php";
                final String tk = cc.sendPost(tokenUrl, "username=" + userName + "&password=" + userPass + "&service=moodle_mobile_app");

                final JSONObject j = getJsonObj(tk);
                if (j.has("error")) {
                    System.exit(1);
                }

                token = getToken(j);
                jobj.put("token", token);

                try {
                    final Writer output = new BufferedWriter(new FileWriter(subJson));
                    final FileWriter fw = new FileWriter(subJson, true);
                    final BufferedWriter bw = new BufferedWriter(fw);
                    final PrintWriter wr = new PrintWriter(bw);
                    final String line = "" + jobj;
                    wr.println(line);
                    wr.close();

                    output.close();

                } catch (IOException e) {
                    JOptionPane.showMessageDialog(new JFrame(), e.getMessage());
                }

            } else {
                token = getToken(jobj);
            }
        } else {
            firstStart();
        }
    }

    private static void createBat() {
        final File bat = new File("C:\\Users\\" + USERNAME + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup", "CourseCon.bat");
        if (!(bat.exists())) {
            try {
                final Writer output = new BufferedWriter(new FileWriter(bat));

                output.close();

                final FileWriter fw = new FileWriter(bat, true);
                final BufferedWriter bw = new BufferedWriter(fw);
                final PrintWriter wr = new PrintWriter(bw);
                //String line = "start /d \"C:\\Program Files\\CourseCon\\\" CourseCon.exe";
                final String line = "start javaw -jar -Xms1024m -Xmx1024m \"C:\\Users\\" + USERNAME + "\\Documents\\CourseCon\\CourseCon.jar\"";
                wr.println(line);
                wr.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void firstStart() throws Exception {
        final JPanel panel = new JPanel(new GridLayout(0, 1));
        final JTextField nameField = new JTextField(10);
        final JPasswordField passField = new JPasswordField(10);
        final JLabel inst = new JLabel("Enter your username and password");
        final JLabel nameText = new JLabel("Username");
        final JLabel passText = new JLabel("Password");

        panel.add(inst);
        panel.add(nameText);
        panel.add(nameField);
        panel.add(passText);
        panel.add(passField);

        final int h = JOptionPane.showConfirmDialog(null, panel, "Add Item", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (h == JOptionPane.OK_OPTION) {
            final String passTxt = passField.getText();
            final String nameTxt = nameField.getText();
            final String tokenUrl = ourvle + "/login/token.php";
            final String tk = cc.sendPost(tokenUrl, "username=" + nameTxt + "&password=" + passTxt + "&service=moodle_mobile_app");

            final JSONObject tokenJson =  new JSONObject(tk);
            if (tokenJson.has("error")) {
                JOptionPane.showMessageDialog(new JFrame(), tokenJson.getString("error"));
                firstStart();
            } else {
                createJson(nameTxt, passTxt);
            }
        } else {
            System.exit(0);
        }

    }

    private static void createJson(String uName, String uPass) throws Exception {
        try {
            String path;

            if (OS.equals("linux")) {
                path = "/home/" + USERNAME + "/.CourseCon";
            } else {
                path = "C:\\Users\\" + USERNAME + "\\Documents\\CourseConFiles";
            }

            final File dir = new File(path);
            dir.mkdir();

            final Writer output = new BufferedWriter(new FileWriter(subJson));

            output.close();
            subJson.createNewFile();
            final FileWriter fw = new FileWriter(subJson, true);
            final BufferedWriter bw = new BufferedWriter(fw);
            final PrintWriter wr = new PrintWriter(bw);
            String line;
            if (OS.equals("linux")) {
                line = "{\"path\":\"/home/\",\"path2\":\"/\",\"username\":\"" + uName + "\",\"password\":\"" + uPass + "\",\"token\":null}";
            } else {
                line = "{\"path\":\"C:\\\\Users\\\\\",\"path2\":\"\\\\Documents\\\\\",\"username\":\"" + uName + "\",\"password\":\"" + uPass + "\",\"token\":null}";
            }
            wr.println(line);
            wr.close();
            config();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void trayView() throws IOException {
        trayIcon = null;
        if (SystemTray.isSupported()) {
            // get the SystemTray instance
            final SystemTray tray = SystemTray.getSystemTray();
            // load an image
            final URL m = CourseCon.class.getResource("1755153.jpg");
            final BufferedImage background = ImageIO.read(CourseCon.class.getResource("1755153.jpg"));
            final Image image = (Image) background;
            // create a action listener to listen for default action executed on the tray icon

            final ActionListener men = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            };
            // create a popup menu
            final PopupMenu popup = new PopupMenu();
            // create menu item for the default action
            final MenuItem defaultItem = new MenuItem("Exit");
            defaultItem.addActionListener(men);

            popup.add(defaultItem);
            /// ... add other items
            // construct a TrayIcon
            trayIcon = new TrayIcon(image, "CourseCon", popup);
            // set the TrayIcon properties

            // ...
            // add the tray image
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println(e);
            }

        } else {
            /* empty */
        }
    }
}
