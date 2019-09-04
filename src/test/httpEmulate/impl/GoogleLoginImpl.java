package test.httpEmulate.impl;

import java.io.*;
import java.security.KeyManagementException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import test.httpEmulate.IGoogleLogin;

/**
 * google login implement
 * @author tiantc
 *
 */
public class GoogleLoginImpl implements IGoogleLogin {
	/**
	 * a main config for http emulation
	 */
    protected static Properties configprops = new Properties();
    /**
     * host of proxy
     */
    private static String host;
    /**
     * port of proxy
     */
    private static int port = 80;
    /**
     * proxy to access google
     */
    private HttpHost proxy;
    
    protected Random randGen = new Random();
    
    protected HttpClient httpclient = null;
    
    protected CookieStore cookieStore = null;
    
    protected String googleAccountName;
    protected String googleAccountNameEncoded;
    
    protected String googleAccountPwd;
    
    public static boolean isDebug = true;
    
    protected String logFilePath;
    
    /**
     * to store some key-value for later use.
     */
    protected Map<String, String> map = new HashMap<String, String>();
	
	
    public GoogleLoginImpl() throws Exception {
		super();
		if(!init()) {
			throw new Exception("init failed");
		}
    }
   
    /**
     * init and then login
     */
    private Boolean init() {
    	Properties p = new Properties();
        try {
        	p.load(new FileInputStream("src/config.properties"));
        	configprops.load(new FileInputStream(p.getProperty("httpEmulateConfig")));
        	logFilePath = configprops.getProperty("logFilePath");
        	googleAccountName = configprops.getProperty("googleAccountName");
        	googleAccountPwd = configprops.getProperty("googleAccountPwd");
        	googleAccountNameEncoded = googleAccountName.replace("@", "%40");
        	isDebug = "true".equalsIgnoreCase(configprops.getProperty("isDebug"));
        	
        	host = configprops.getProperty("host");
        	port = Integer.parseInt(configprops.getProperty("port"));
        	proxy = new HttpHost(host, port);
//        	GGroupName = configprops.getProperty("ggroupname");
//        	highFreTerms = ComFileUtil.readFile2String(new File(configprops.getProperty("highFrequenceTermsLoc"))).split("(\r\n|\n\r|\r|\n)+");
//        	outputFile = new File(configprops.getProperty("queryResultOutput"));
    		this.httpclient = new DefaultHttpClient();
            enableSSL(httpclient);
            setHttpClientProxy(httpclient);
            cookieStore = ((AbstractHttpClient) httpclient).getCookieStore();
			return login();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			
			if(isDebug) System.out.println("------------------------------------------initOver----------------------------------------------");
		}
    }
    
    /**
     * login and init some parameter and cookie
     * @throws Exception
     */
    public Boolean login() {
    	boolean isLoginSuccessful = false;
    	try {
//    		homepage();
//			Thread.sleep(1000 * (1 + randGen.nextInt(2)));
			LoginPage();
			Thread.sleep(1000 * (1 + randGen.nextInt(3)));
			getAccountInfo();
			Thread.sleep(1000 * (1 + randGen.nextInt(3)));
			doLogin();
			if(cookieStore.getCookies().size() > 9) {
	        	System.out.println("Login successfully!!!!!!!!!!!!!!!!");
	        	isLoginSuccessful = true;
	        } else {
	        	System.out.println("Login Failed!!!!!!!!!!!!!!!!");
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	return isLoginSuccessful;
    }
	
    /**
     * use proxy to access google
     * @param httpclient
     */
	private void setHttpClientProxy(HttpClient httpclient) {
		httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
	}
	
	/**
	 * accept all https host
	 * @param httpclient
	 * @throws Exception 
	 */
	private static void enableSSL(HttpClient httpclient) throws Exception {
		SSLContext sslcontext = SSLContext.getInstance("TLS");
		sslcontext.init(null, new TrustManager[] { truseAllManager }, null);
		SSLSocketFactory sf = new SSLSocketFactory(sslcontext);
		sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		Scheme https = new Scheme("https", sf, 443);
		httpclient.getConnectionManager().getSchemeRegistry().register(https);
	}
	
	/**
	 * override validate method to trust all cer
	 */
	private static TrustManager truseAllManager = new X509TrustManager() {

		public void checkClientTrusted(
				java.security.cert.X509Certificate[] arg0, String arg1)
				throws CertificateException {

		}

		public void checkServerTrusted(
				java.security.cert.X509Certificate[] arg0, String arg1)
				throws CertificateException {

		}

		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return null;
		}

	};
	
	
	/**
	 * decode  gzip stream
	 * @param is
	 * @return decoded string
	 * @throws Exception 
	 */
	private static String decodeGzipStream(InputStream is) throws Exception {
		return readStream2String(new GZIPInputStream(is));
	}
	
	/**
	 * get content String from entity
	 * @param entity
	 * @return content String
	 * @throws Exception
	 */
	protected static String getContentStr(HttpEntity entity) throws Exception {
		String contentEncoding = "" + entity.getContentEncoding();
		//if it is gzip format
		if(contentEncoding.toLowerCase().indexOf("gzip") > -1) {
//			System.out.println("gzip format");
			return decodeGzipStream(entity.getContent());
		}
//		System.out.println("none gzip format " + contentEncoding + "   | " + entity.getContentType().getName() + "  | " + entity.getContentType().getValue());
		return EntityUtils.toString(entity, encode);
	}
	
	/**
	 * get resList from result string
	 * @param resStr
	 * @return
	 */
	protected static List<String> getResList(String resStr) {
		if(isDebug) {
			System.out.println("original:\n");
			System.out.println(resStr);
		}
		List<String> resList = new ArrayList<String>();
		List<String> str2List = null;
		try {
			String arrStr = resStr.substring(resStr.indexOf("[", 6) + 2, resStr.length() - 7);
			str2List = Arrays.asList(resStr.split("\",\""));
			int size = str2List.size();
			int i = 0;
			for(i = 0; i < size; i++) {
				try {
					if(getMatchedString(str2List.get(i), "^(\\w|-){2,5}\\.\\d{5}$") == null) {
						//ComLogUtil.error(str2List.get(i) + " not matched");
					} else {
						resList.add(str2List.get(i));
					}
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
			//ComLogUtil.printList(resList, "");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		str2List = null;
		return resList;
	}
	
	/**
	 * construct a cookie string for header
	 */
	protected String getAllCookieStr4Header() {
		List<Cookie> cookies = cookieStore.getCookies();
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < cookies.size(); i++) {
			if(i > 0) sb.append("; "); 
			sb.append(cookies.get(i).getName()).append("=").append(cookies.get(i).getValue());
		}
		if(isDebug) System.out.println("cookieStr:" + sb.toString());
		return sb.toString();
	}
	
	/**
	 * construct a cookie string for payload
     */
	protected String getAllCookieStr4Payload() {
	    List<Cookie> cookies = cookieStore.getCookies();
	    StringBuilder sb = new StringBuilder();
	    for(int i = 0; i < cookies.size(); i++) {
	    	sb.append("&").append(cookies.get(i).getName()).append("=").append(cookies.get(i).getValue());
	    }
	    if(isDebug) System.out.println("cookieStr4Payload:" + sb.toString());
	    return sb.toString();
	}
	    
	/**
	 * print all cookie stored in httpClient
	 */
	protected void printAllCookie(String grepMark) {
		if(isDebug) printIterator(cookieStore.getCookies(), grepMark);
	}
	
	/**
	 * do Login
	 */
	private void doLogin() throws Exception {
		HashMap<String, String>headerMap = new HashMap<String, String>();
		headerMap.put("Host", "accounts.google.com");
		headerMap.put("Content-Type", "application/x-www-form-urlencoded");
		
		
		HttpPost httpPost = new HttpPost("https://accounts.google.com/signin/challenge/sl/password");
		StringBuilder payloadStr = new StringBuilder("Page=PasswordSeparationSignIn")
										.append(getAllCookieStr4Payload())
										.append("&gxf=").append(map.get("gxf"))
										.append("&continue=").append(map.get("continue"))
										.append("&bgresponse=").append(map.get("bgresponse"))
										.append("&_utf8=%E2%98%83")
										.append("&pstMsg=1")
										.append("&Email=").append(googleAccountNameEncoded)
										.append("&Passwd=").append(googleAccountPwd)
										.append("&PersistentCookie=yes")
										.append("&rmShown=1")
										.append("&ProfileInformation=").append(map.get("encoded_profile_information"));
		
        if(isDebug) System.out.println("payloadStr:" + payloadStr);
         
		String resStr = this.executePost(httpPost, headerMap, payloadStr.toString());
        if(isDebug) writeString2File(resStr, new File(logFilePath + "/doLogin.txt"));
        
	}
	
	/**
	 * get account Info
	 */
	private void getAccountInfo() throws Exception {
		HashMap<String, String>headerMap = new HashMap<String, String>();
		headerMap.put("Host", "accounts.google.com");
		headerMap.put("Content-type", "application/x-www-form-urlencoded");
		
		StringBuilder payloadStr = new StringBuilder("Email=").append(googleAccountNameEncoded)
				.append(getAllCookieStr4Payload())
				.append("&GALX=").append(map.get("GALX"))
				.append("&gxf=").append(map.get("gxf"))
				.append("&continue=").append(map.get("continue"))
				.append("&bgresponse=").append(map.get("bgresponse"))
				.append("&_utf8=%E2%98%83")
				.append("&pstMsg=1")
				.append("&Passwd=").append(googleAccountPwd)
				.append("&PersistentCookie=yes")
				.append("&rmShown=1")
				.append("&Page=PasswordSeparationSignIn")
				.append("&requestlocation=https%3A%2F%2Faccounts.google.com%2FServiceLogin%3Fhl%3Dzh-CN%26passive%3Dtrue%26continue%3Dhttps%3A%2F%2Fwww.google.com.au%2F%253Fgws_rd%253Dssl%23identifier");
		
		if(isDebug) System.out.println("payloadStr:" + payloadStr);
		
        HttpPost httpPost = new HttpPost("https://accounts.google.com/accountLoginInfoXhr");
        
        String resStr = this.executePost(httpPost, headerMap, payloadStr.toString());
		
        //httpPost.addHeader("Referer", "https://accounts.google.com/ServiceLogin?hl=zh-CN&passive=true&continue=https://www.google.com.au/%3Fgws_rd%3Dssl");
        
        if(isDebug) writeString2File(resStr, new File(logFilePath + "/getAccountInfo.txt"));
        
        //store some info for later http request use.
        JSONObject jsonObject = new org.json.JSONObject(resStr);
        map.put("encoded_profile_information", jsonObject.getString("encoded_profile_information"));
        if(isDebug) System.out.println(jsonObject.get("encoded_profile_information"));
        
        
	}
	
	/**
	 * init login page
	 */
	private void LoginPage() throws Exception {
		HashMap<String, String> headerMap = new HashMap<String, String>();
		headerMap.put("Host", "accounts.google.com");
        HttpGet httpGet = new HttpGet("https://accounts.google.com/ServiceLogin?hl=zh-CN&passive=true&continue=https://www.google.com.au/%3Fgws_rd%3Dssl");
        
        String resStr = this.executeGet(httpGet, headerMap);
        if(isDebug) writeString2File(resStr, new File(logFilePath + "/LoginPage.txt"));
        Document doc = Jsoup.parse(resStr);
		Elements allElements = doc.getElementsByTag("input");
		//System.out.println(allElements.select("input[name=GALX]").get(0).attr("value"));
		map.put("GALX", allElements.select("input[name=GALX]").get(0).attr("value"));
		map.put("gxf", allElements.select("input[name=gxf]").get(0).attr("value"));
		map.put("continue", allElements.select("input[name=continue]").get(0).attr("value"));
		map.put("hl", allElements.select("input[name=hl]").get(0).attr("value"));
		map.put("_utf8", allElements.select("input[name=_utf8]").get(0).attr("value"));
		map.put("bgresponse", allElements.select("input[name=bgresponse]").get(0).attr("value"));	//js_disabled
	}
	
	
	private static Map<String, String> defaultHeaderMap = new HashMap<String, String>();
	private static Set<String> defaultHeaderSet;
	static {
		defaultHeaderMap.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36");
		defaultHeaderSet = defaultHeaderMap.keySet();
	}

	/**
	 * execute HttpPost
	 * @param httpRequest
	 * @param headers
	 * @param entityStr
	 * @return
	 * @throws Exception
	 */
	protected String executePost(HttpPost httpPost, Map<String, String> headers, String entityStr) throws Exception {
		httpPost.setEntity(new StringEntity(entityStr));
		return this.executeGet(httpPost, headers);
	}
	
	/**
	 * execute HttpGet.
	 * @param httpRequest
	 * @param headers
	 * @return
	 * @throws Exception
	 */
	protected String executeGet(HttpRequestBase httpGet, Map<String, String> headers) throws Exception {
		if(isDebug) System.out.println("");
		Set<String> keySet = headers.keySet();
		Iterator<String> it = keySet.iterator();
		String key;
		//add customized header.
		while(it.hasNext()) {
			key = it.next();
			httpGet.addHeader(key, headers.get(key));
		}
		it = defaultHeaderSet.iterator();
		//add default header. but won't cover the customized header
		while(it.hasNext()) {
			key = it.next();
			if(!headers.containsKey(key))
				httpGet.addHeader(key, headers.get(key));
		}
		httpGet.addHeader("Cookie", getAllCookieStr4Header());
		
		HttpResponse response = executeAgainOnFail(httpGet);
		if(isDebug) System.out.println("statusCode:" + response.getStatusLine().getStatusCode());
        
        HttpEntity entity = response.getEntity();
		org.apache.http.Header contentType = entity.getContentType();
		if(isDebug) System.out.println(new StringBuilder("ContentType:").append(contentType.getName()).append("  ").append(contentType.getValue()));
        
		String resStr = getContentStr(entity);
		httpGet.releaseConnection();
        printAllCookie("Cookie:");
        if(isDebug) System.out.println("-----------------------------------------------------------");
		return resStr;
	}
	
	
	/**
	 * Reexecute 5 times if failed 
	 * @param httpRequest
	 * @return
	 * @throws Exception
	 */
	private HttpResponse executeAgainOnFail(HttpRequestBase httpRequest) throws Exception {
		Exception e = null;
		for(int i = 0; i< 5; i++) {
			try {
				return httpclient.execute(httpRequest);
			} catch (Exception e1) {
				e = e1;
			}
		}
		throw e;
	}
	
	public static void main(String[] args) {
		try {
			GoogleLoginImpl ggq = new GoogleLoginImpl();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		ggq.generateQueryResult();
		
	}
	
	
	//util
	
	/**
	 * This only apply for text stream.
	 * @param is 
	 * @return
	 * @throws Exception
	 */
	public static String readStream2String(InputStream is) throws Exception {
		StringBuilder resStr = new StringBuilder();
		String readLine = null;
		BufferedReader br = null;
		InputStreamReader isr = null;
		try {
			isr = new InputStreamReader(is, encode);
			br = new BufferedReader(isr);
			while((readLine = br.readLine()) != null) {
				resStr.append(readLine).append("\n");
			}
		} finally {
			if(br != null) {
				br.close();
				br = null;
			}
			if(isr != null) {
				isr.close();
				isr = null;
			}
		}
		return resStr.toString();
	}
	
	  /**
	   * @param String
	   * @param file
	   * @param encode
	   * @return
	   * @throws Exception
	   */
	  public static void writeString2File(String str, String file) throws Exception {
		  writeString2File(str, new File(file));
	  }
	  
	  /**
	   * @param String
	   * @param file
	   * @param encode
	   * @return
	   * @throws Exception
	   */
	  public static void writeString2File(String str, File file) throws Exception {
		  BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encode));
		  writer.write(str);
		  writer.flush();
		  writer.close();
	  }

	  public static void printIterator(Iterable iterable, String grepMark) {
		  Iterator it = iterable.iterator();
		  Object tmp = null;
		  int i = 0;
		  while(it.hasNext()) {
			  tmp = it.next();
			  System.out.println(grepMark + "index:" + i + " value:" + tmp);
			  i++;
		  }
	  }
	  
	  /**
	   * get first Matched String from sourceStr
	   * 
	   * @param sourceStr
	   * @param regex
	   * @return the first matched string; null if not matched.
	   */
	  public static String getMatchedString(String sourceStr, Object regex){
	    Matcher matcher = Pattern.compile(regex + "").matcher(sourceStr);
	    if(matcher.find()){
	      return matcher.group(0);
	    }
	    return null;
	  }
	
		public static String readFile2String(File file) throws Exception {
			return readStream2String(new FileInputStream(file));
		}
		
		public static String readFile2String(String file) throws Exception {
	    return readFile2String(new File(file));
	  }
		
		/**
		 * append string into file
		 * @param is
		 * @param file
		 * @return
		 * @throws Exception
		 */
		public static void appendString2File(String str, File file) throws Exception {
			FileWriter fw = null;
			try {
				fw = new FileWriter(file, true);
				fw.write(str);
				fw.flush();
			} finally {
				if (fw != null) {
					fw.close();
					fw = null;
				}
			}

		}
		
		/**
		 * get file name 
		 * @param path
		 * @param hasExtension keep extension name. 
		 * @return
		 * @throws Exception
		 */
		public static String getFileName(String path, Boolean hasExtension) throws Exception {
			return getMatchedString(path, hasExtension ? "(?<=/?)(?!(.*/.*))[^?/]*" : "(?<=/?)(?!(.*/.*))[^?/\\.]*");
		}
		
		/**
		 * get file extension
		 * 
		 * @param path
		 * @param needDot
		 *            true:.jpg; false:jpg.
		 * @return
		 * @throws Exception
		 */
		public static String getFileExtension(String path, Boolean needDot) throws Exception {
			return getMatchedString(path, needDot ? "\\.(?!(.*\\..*)).*" : "(?<=\\.)(?!(.*\\..*)).*");
		}
		
}