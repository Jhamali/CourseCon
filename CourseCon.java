
/**
 * Created by dubze on 1/12/2017.
 */
import java.awt.GridLayout;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CourseCon {

    static String name, userName, userPass, token, subPath, user;
    private static final String ourvle = "http://ourvle.mona.uwi.edu/";
    static int subjectId, id;
    static File bat, subJson;
    static CourseCon cc = new CourseCon();

    private final String USER_AGENT = "Mozilla/5.0";

    public static void main(String[] args) throws Exception {

        createBat();

        config();

        

        String info = cc.sendGet(ourvle + "webservice/rest/server.php?wstoken=" + token + "&wsfunction=core_webservice_get_site_info&moodlewsrestformat=json");

        id = parseID(getJsonObj(info));

        String sub = cc.sendGet(ourvle + "webservice/rest/server.php?wsfunction=core_enrol_get_users_courses&moodlewsrestformat=json&userid=" + id + "&wstoken=" + token);

        JSONArray subs = getJsonArray(sub);

        int EOF = 0;
        while (EOF == 0) {
            parseSubjects(subs);
            try {
                Thread.sleep(3600000);
            } catch (InterruptedException e) {
                JOptionPane.showMessageDialog(new JFrame(), e.getMessage());
            }
        }

    }

    // HTTP GET request
    private String sendGet(String url) throws Exception {

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        String str = response.toString();
        if (str.equals("{\"error\":\"The username was not found in the database\",\"stacktrace\":null,\"debuginfo\":null,\"reproductionlink\":null}")) {
            return "Fail";
        } else {
            return str;
        }
    }

    // HTTP POST request
    private static String sendPost(String url, String param) throws Exception {

        url = ourvle + "login/token.php?";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //add reuqest header
        con.setRequestMethod("POST");
        //con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        String urlParameters = param;

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        String str = response.toString();
        if (str.equals("{\"error\":\"The username was not found in the database\",\"stacktrace\":null,\"debuginfo\":null,\"reproductionlink\":null}")) {
            return "Fail";

        } else {
            return str;
        }
    }

    static JSONObject getJsonObj(String str) {
        JSONObject ob = new JSONObject(str);

        return ob;
    }

    static JSONArray getJsonArray(String str) {
        JSONArray ob = new JSONArray(str);

        return ob;
    }

    static String getToken(JSONObject ob) {
        String result = ob.getString("token");
        return result;
    }

    static int parseID(JSONObject info) {
        int result = info.getInt("userid");
        
        return result;
    }

    static String parseUserName(JSONObject info) {
        String result = info.getString("username");
        
        return result;
    }

    static void parseSubjects(JSONArray subs) throws Exception {
        
        for (int i = 0; i < subs.length(); i++) {
            name = (subs.getJSONObject(i)).getString("shortname");
            String path = subPath + name;
            subjectId = (subs.getJSONObject(i)).getInt("id");
            File file = new File(path);
            file.mkdir();
            subFld(name, subjectId);

        }
    }

    static void subFld(String fldName, int subId) throws Exception {
        String subData;
        try {
            subData = cc.sendGet(ourvle + "webservice/rest/server.php?courseid=" + subId + "&moodlewsrestformat=json&wstoken=" + token + "&wsfunction=core_course_get_contents");
        } catch (JSONException e) {
            JOptionPane.showMessageDialog(new JFrame(), e.getMessage());
            subData = null;
        }
        JSONArray subArray = getJsonArray(subData);
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

    static void fileDownload(String fld, JSONObject jsonFiles) throws IOException {

        if (!jsonFiles.isNull("filename")) {
            String u = "" + jsonFiles.getString("fileurl") + "&token=" + token;
            

            try {
                URL website = new URL(u);
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());

                String filePath = subPath + "" + fld + "\\" + jsonFiles.getString("filename");
                String downPath = filePath.replaceAll("\\s+", "");
                
                File down = new File(downPath);
                if (!(down.exists())) {
                    FileOutputStream fos = new FileOutputStream(downPath);

                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                } 
            } catch (FileNotFoundException e) {
                
            }

        }
    }

    static void config() throws Exception {

        String result = "";
        subJson = new File("C:\\Users\\" + user + "\\Documents\\Http\\config.json");
        if (subJson.exists()) {

            try {
                BufferedReader br = new BufferedReader(new FileReader(subJson));
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    line = br.readLine();
                }
                result = sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }

            JSONObject jobj = new JSONObject(result);

            userName = jobj.getString("username");
            userPass = jobj.getString("password");
            subPath = "" + jobj.getString("path") + user + jobj.getString("path2");
            if (jobj.isNull("token")) {
                String tokenUrl = ourvle + "login/token.php?username=" + userName + "&password=" + userPass + "&service=moodle_mobile_app";
                String tk = cc.sendGet(tokenUrl);
                JSONObject j = getJsonObj(tk);
                token = (getToken(j));
                jobj.remove("token");
                jobj.put("token", token);

                try {
                    Writer output = null;

                    output = new BufferedWriter(new FileWriter(subJson));
                    FileWriter fw = new FileWriter(subJson, true);
                    BufferedWriter bw = new BufferedWriter(fw);
                    PrintWriter wr = new PrintWriter(bw);
                    String line = "" + jobj;
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

    static void createBat() {
        user = System.getProperty("user.name");
        File bat = new File("C:\\Users\\" + user + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup", "CourseCon.bat");
        if (!(bat.exists())) {
            try {
                Writer output = null;

                output = new BufferedWriter(new FileWriter(bat));

                output.close();
                
                FileWriter fw = new FileWriter(bat, true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter wr = new PrintWriter(bw);
                String line = "start /d \"C:\\Program Files\\CourseCon\\\" CourseCon.exe";
                wr.println(line);
                wr.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } 
    }

    static void firstStart() throws Exception {

        JPanel panel = new JPanel(new GridLayout(0, 1));
        JTextField nameField = new JTextField(10);
        JPasswordField passField = new JPasswordField(10);
        JLabel inst = new JLabel("Enter your username and password");
        JLabel nameText = new JLabel("Username");
        JLabel passText = new JLabel("Password");

        panel.add(inst);
        panel.add(nameText);
        panel.add(nameField);
        panel.add(passText);
        panel.add(passField);

        int h = JOptionPane.showConfirmDialog(null, panel, "Add Item", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (h == JOptionPane.OK_OPTION) {
            String passTxt = passField.getText();
            String nameTxt = nameField.getText();
            String tokenUrl = ourvle + "login/token.php?";
            String tokenParam = "username=" + nameTxt + "&password=" + passTxt + "&service=moodle_mobile_app";
            String tk = cc.sendPost(tokenUrl, tokenParam);
            if (tk.equals("Fail")) {
                JOptionPane.showMessageDialog(new JFrame(), "Incorrect username and/or password");
                firstStart();
            } else {
                createJson(nameTxt, passTxt);
            }
        } else {
            System.exit(0);
        }

    }

    static void createJson(String uName, String uPass) throws Exception {

        try {

            File dir = new File("C:\\Users\\dubze\\Documents\\Http");
            dir.mkdir();
            Writer output = null;

            output = new BufferedWriter(new FileWriter(subJson));

            output.close();
            subJson.createNewFile();
            FileWriter fw = new FileWriter(subJson, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter wr = new PrintWriter(bw);
            String line = "{    \"path\":\"C:\\\\Users\\\\\",\"path2\":\"\\\\Documents\\\\\",\"username\":\"" + uName + "\",\"password\":\"" + uPass + "\",\"token\":null\n" + "}";
            wr.println(line);
            wr.close();
            config();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
