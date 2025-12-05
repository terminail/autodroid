
    package com.autodroid.test;
    
    import java.net.HttpURLConnection;
    import java.net.URL;
    import java.io.InputStream;
    
    public class NetworkTest {
        public static void main(String[] args) {
            try {
                // 测试10.0.2.2连接
                URL url = new URL("http://10.0.2.2:8004/api/health");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                
                int responseCode = conn.getResponseCode();
                System.out.println("Response Code: " + responseCode);
                
                if (responseCode == 200) {
                    InputStream is = conn.getInputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    StringBuilder response = new StringBuilder();
                    while ((bytesRead = is.read(buffer)) != -1) {
                        response.append(new String(buffer, 0, bytesRead));
                    }
                    System.out.println("Response: " + response.toString());
                }
                
                conn.disconnect();
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    