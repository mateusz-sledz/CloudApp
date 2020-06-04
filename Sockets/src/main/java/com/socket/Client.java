package com.socket;

import javax.net.ssl.*;
import java.io.*;
import java.security.cert.X509Certificate;
import java.util.Arrays;


public class Client {

    private SSLSocket clientSocket;
    private DataOutputStream outToServer;
    private BufferedReader inFromServer;
    private DataInputStream in;
    private String authCode;


    public Client(){

        try {
            this.clientSocket = init_socket();
            this.outToServer = new DataOutputStream(clientSocket.getOutputStream());
            this.inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream())); // , buffersize
            this.authCode = "";
        }

        catch(Exception e){
            e.printStackTrace();
        }
    }

    private SSLSocket init_socket(){
        try {

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new TrustAllX509TrustManager()}, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String string, SSLSession ssls) {
                    return true;
                }
            });

            SSLSocketFactory socketFactory = sc.getSocketFactory();

            return (SSLSocket) socketFactory.createSocket("77.55.211.26", 12346);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return clientSocket;
    }

    public boolean authorize(String username, String password){

        String authJSON = "{\"username\":\""+ username +"\", \"password\":\""+ password +"\"}";

        String sentence = "POST /authorize HTTP/1.1\r\n" +
                "Host: 77.55.211.26:12345\r\n" +
                "Content-Length: " + authJSON.length() + "\r\n" +
                "Connection: Keep-Alive\r\n" +
                "Keep-Alive: timeout=5, max=1000\r\n" +
                "Content-Type: application/json\r\n\r\n";
        try {

            String fromServer = send(sentence, authJSON);

            String[] headers = fromServer.split("\r\n");
            String[] status = headers[0].split(" ");

            if(!status[1].equals("200")){
                this.authCode = "-1";
                System.out.println("Authorization failed!");
                return false;
            }

            String[] authorisation = headers[3].split("BEARER");

            this.authCode = authorisation[1];

            System.out.println(authCode);

            System.out.println("Authorization successfull!");
            return true;
        }

        catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }


    private void restart(){
        try{
            clientSocket.close();
            this.clientSocket = init_socket();
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
        }

        catch(Exception e ){
            e.printStackTrace();
        }
    }

    private boolean isAlive(){
        try {
            String sentence = "GET /dir/asd HTTP/1.1\r\n" +
                    "Authorization:" + authCode +
                    "\r\n\r\n";

            byte[] data = sentence.getBytes();

            outToServer.write(data);

            char[] received = new char[1000];
            int len = inFromServer.read(received, 0, 1000);

            if(len <= 0){
                return false;
            }

            return true;
        }

        catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public String[] getDirs(String path){

        String sentence = "GET /dir" + path + " HTTP/1.1\r\n" +
                "Authorization:" + authCode +
                "\r\n\r\n";

        System.out.println("GETDIRS sentence" + sentence);
        String x = send(sentence, "");

        Parser parser = new Parser(x);

        return parser.getDirs();
    }

    public void get_dirs_info(String path){

        String info = "?info=True";

        String sentence = "GET /dir" + path + info +  " HTTP/1.1\r\n" +
                "Authorization:" + authCode +
                "\r\n\r\n";

        String x = send(sentence, "");

        Parser parser = new Parser(x);

        System.out.println(parser.getBody());

    }

    public void delete(String path, String item){

        String sentence;

        if(item.contains(".")){
            sentence = "DELETE /file" + path + item + " HTTP/1.1\r\n" +
                    "Authorization:" + authCode + "\r\n" +
                    "Content-Length: 0" +
                    "\r\n\r\n";
        }
        else{
            sentence = "DELETE /dir" + path +"?recursive=True" + " HTTP/1.1\r\n" +
                    "Authorization:" + authCode + "\r\n" +
                    "Content-Length: 0" +
                    "\r\n\r\n";

        }

        String x = send(sentence, "");
        Parser parser = new Parser(x);

        System.out.println(parser.getHeaders());
    }


    private String send(String headers, String body){
        try {

            if(!authCode.equals("")) {

                if (!isAlive()) {
                    restart();
                }
            }

            String sentence = headers + body;

            byte[] array = sentence.getBytes();
            outToServer.write(array);

            char[] received = new char[1024*8];
            int len = inFromServer.read(received, 0, 1024*8);


            if(len <= 0){
                System.out.println("Server didn't send any response");
                return "";
            }

            char[] newarray2 = Arrays.copyOfRange(received, 0, len);

            return new String(newarray2);
        }

        catch(Exception e){
            e.printStackTrace();
            return "";
        }
    }


    public void mkdir(String path){

        String sentence = "POST /dir" + path + " HTTP/1.1\r\n" +
                "Authorization:" + authCode + "\r\n" +
                "Content-Length: 0"+
                "\r\n\r\n";

        String x = send(sentence, "");
    }

    public void upload_range(String file, String path){
        if(!authCode.equals("")) {
            if (!isAlive()) {
                restart();
            }
        }

        try{

            long bytes = new File(file).length();

            String temp = Long.toString(bytes);

            int to_send = Integer.parseInt(temp) ;

            int already_send = 0;
            int package_len = 4096*16;
            int i = 0;
            int j = package_len - 1;
            FileInputStream filein = new FileInputStream(file);

            String sentence ;

            if(to_send < package_len){
                j = to_send - 1;
                package_len = to_send;

                sentence = "POST /file" + path + " HTTP/1.1\r\n" +
                        "Authorization:" + authCode + "\r\n" +
                        "Content-Range: " + "bytes " + i + "-" + j + "/" + to_send +
                        "\r\n\r\n";

                byte[] array = sentence.getBytes();
                outToServer.write(array);

                byte[] data = new byte[package_len];
                filein.read(data);

                outToServer.write(data);
                read_headers();
                return;
            }

            while(already_send < to_send) {

                sentence = "POST /file" + path + " HTTP/1.1\r\n" +
                        "Authorization:" + authCode + "\r\n" +
                        "Content-Range: " + "bytes " + i + "-" + j + "/" + to_send + "\r\n"+
                        "Content-Length: " + package_len +
                        "\r\n\r\n";

                System.out.println(sentence);

                byte[] array = sentence.getBytes();
                outToServer.write(array);

                byte[] data = new byte[package_len];
                filein.read(data);

                outToServer.write(data);
                read_headers();

                already_send += package_len;
                i += package_len;

                if (j + package_len > to_send) {
                    package_len = to_send - j ;
                    j = to_send -1;
                } else
                    j += package_len;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    String readline(){

        try{

            ByteArrayOutputStream output = new ByteArrayOutputStream();


            while(true){

                byte x = in.readByte();

                byte y = 0x0A;

                if( Byte.compare(x, y) == 0){
                    return output.toString();
                }
                output.write(x);

            }

        }
        catch(Exception e){
            e.printStackTrace();

        }
        return "";
    }

    String read_headers(){

        StringBuilder headers = new StringBuilder();

        while(true){

            String header = readline();

            if(header.equals("\r")){
                return headers.toString();
            }

            headers.append(header);
            headers.append("\n");
        }
    }


    public void send_range(String server_path, String localpath) {

        if (!authCode.equals("")) {
            if (!isAlive())
                restart();
        }

        int i = 0;
        int j = 4096*16-1;
        int already_read = 0;

        try {

            FileOutputStream out = new FileOutputStream(localpath);

            String sentence = "GET /file" + server_path + " HTTP/1.1\r\n" +
                    "Range: bytes=" + i + "-" + j + "\r\n" +
                    "Authorization:" + authCode +
                    "\r\n\r\n";

            byte[] array = sentence.getBytes();

            outToServer.write(array);

            String headers = read_headers();
            Parser parser = new Parser(headers);

            String content_range2 = parser.get_content_range(parser.getHeaderValue("Content-Range"));

            int to_read = Integer.parseInt(content_range2);

            int content_len = Integer.parseInt(parser.getHeaderValue("Content-Length"));

            byte [] data = new byte[content_len];

            in.readFully(data);

            already_read += content_len;

            System.out.println("to read: " + to_read);

            if(to_read <= content_len){
                out.write(data);
                out.close();
                return;
            }

            out.write(data);

            while(already_read < to_read){

                if (j + 4096*16 >= to_read) {
                    content_len = to_read - j -1 ;
                    j = to_read;
                } else
                    j += 4096*16;

                i += 4096*16;

                sentence = "GET /file" + server_path + " HTTP/1.1\r\n" +
                        "Range: bytes=" + i + "-" + j + "\r\n" +
                        "Authorization:" + authCode +
                        "\r\n\r\n";

                byte[] arr = sentence.getBytes();

                outToServer.write(arr);

                read_headers();

                byte [] bytes = new byte[content_len];

                in.readFully(bytes);

                out.write(bytes);

                already_read += content_len;
            }

            out.close();

            System.out.println("already read: " + already_read);

            System.out.println("i: " + i + "  j:" + j);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class TrustAllX509TrustManager implements X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] certs,
                                       String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs,
                                       String authType) {
        }
    }
}
