package test.httpEmulate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import test.httpEmulate.Model.StringResponse;
import util.commonUtil.ComCollectionUtil;
import util.commonUtil.ComFileUtil;
import util.commonUtil.ComLogUtil;
import util.commonUtil.ComStrUtil;

public class LinkFetcher {

	public static boolean isDebug = true;

	public final static String encode = "utf-8";

	static protected HttpClient httpclient = null;

	/**
     * host of proxy
     */
    private static String host = "localhost";
    /**
     * port of proxy
     */
    private static int port = 1080;
    /**
     * proxy to access google
     */
    private static HttpHost proxy;

	static protected String logFilePath;

	static {
		init();
	}

	/**
     * init and then login
     */
    static private Boolean init() {
    	Properties p = new Properties();
        try {
//        	host = configprops.getProperty("host");
//        	port = Integer.parseInt(configprops.getProperty("port"));
//        	proxy = new HttpHost(host, port);

    		httpclient = new DefaultHttpClient();
    		logFilePath = "d:\\logs\\";
    		proxy = new HttpHost(host, port);
            setHttpClientProxy(httpclient);
//			return login();
    		return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {

			if(isDebug) System.out.println("------------------------------------------initOver----------------------------------------------");
		}
    }

    /**
     * use proxy to access google
     * @param httpclient
     */
	private static void setHttpClientProxy(HttpClient httpclient) {
		httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
	}

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
	 * Reexecute 5 times if failed
	 * @param httpRequest
	 * @return
	 * @throws Exception
	 */
	private static HttpResponse executeAgainOnFail(HttpRequestBase httpRequest) throws Exception {
		Exception e = null;
		for(int i = 0; i< 3; i++) {
			try {
				return httpclient.execute(httpRequest);
			} catch (Exception e1) {
				e = e1;
			}
		}
		throw e;
	}

	/**
	 * execute HttpGet.
	 * @param httpRequest
	 * @param headers
	 * @return
	 * @throws Exception
	 */
	protected static StringResponse executeGet(HttpRequestBase httpGet, Map<String, String> headers) throws Exception {
		if(isDebug) System.out.println("");
		Set<String> keySet = headers.keySet();
		Iterator<String> it = keySet.iterator();
		String key;
		//add customized header.
		while(it.hasNext()) {
			key = it.next();
			httpGet.addHeader(key, headers.get(key));
		}
//		it = defaultHeaderSet.iterator();
//		//add default header. but won't cover the customized header
//		while(it.hasNext()) {
//			key = it.next();
//			if(!headers.containsKey(key))
//				httpGet.addHeader(key, headers.get(key));
//		}
//		httpGet.addHeader("Cookie", getAllCookieStr4Header());

		HttpResponse response = executeAgainOnFail(httpGet);
		int statusCode = response.getStatusLine().getStatusCode();
		if(isDebug) System.out.println("statusCode:" + statusCode);
		if(statusCode != 200) {
			return new StringResponse(statusCode, "");
		}
        HttpEntity entity = response.getEntity();
		org.apache.http.Header contentType = entity.getContentType();
		if(isDebug) System.out.println(new StringBuilder("ContentType:").append(contentType.getName()).append("  ").append(contentType.getValue()));

		String resStr = getContentStr(entity);
		httpGet.releaseConnection();
        if(isDebug) System.out.println("-----------------------------------------------------------");
		return new StringResponse(statusCode, resStr);
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

	public static List<String> fetchList(int pageNum) throws Exception {
		HashMap<String, String> headerMap = new HashMap<String, String>();
		headerMap.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
		headerMap.put("Host", "www.dfidol.com");
		headerMap.put("Upgrade-Insecure-Requests", "1");
		headerMap.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36");
		headerMap.put("Accept-Encoding", "gzip, deflate");
		headerMap.put("Accept-Language", "en-US,en;q=0.9,zh;q=0.8,zh-CN;q=0.7,zh-TW;q=0.6");
//		headerMap.put("Host", "wwm");
//		headerMap.put("Host", "wwm");



        HttpGet httpGet = new HttpGet("http://www.dfidol.com/index.php/page/" + pageNum + "/");
        StringResponse res = executeGet(httpGet, headerMap);
        int statusCode = res.getStatusCode();
        if(statusCode != 200) {
        	if(statusCode == 404) { // then it's the end of pages number
        		ComLogUtil.info("reach the end of availabe page: " + pageNum);
        		// need to return null;
        		return null;
        	} else {
        		String errorMsg = "failed to fetch page:" + pageNum + " while it's not 404. statusCode: " + statusCode;
        		ComLogUtil.info(errorMsg);
        		throw new Exception(errorMsg);
        	}
        }

        String resStr = res.getBody();
        httpGet.abort();
//        System.out.println(resStr);
        if(isDebug) writeString2File(resStr, new File(logFilePath + "/page" + pageNum + ".txt"));
        Document doc = Jsoup.parse(resStr);
//		Elements allElements = doc.getElementsByTag("input");
		Elements posts = doc.select("div[class='post'] > a");
		int size = posts.size();
		List<String> list = new ArrayList<String>();
		for(int i = 0; i < size; i++) {

			Element post = posts.get(i);
			String link = post.attr("href");
			list.add(link);
			if(isDebug) System.out.println("extracted a link:" + link);


		}
		return list;
//		//System.out.println(allElements.select("input[name=GALX]").get(0).attr("value"));
//		map.put("GALX", allElements.select("input[name=GALX]").get(0).attr("value"));
//		map.put("gxf", allElements.select("input[name=gxf]").get(0).attr("value"));
//		map.put("continue", allElements.select("input[name=continue]").get(0).attr("value"));
//		map.put("hl", allElements.select("input[name=hl]").get(0).attr("value"));
//		map.put("_utf8", allElements.select("input[name=_utf8]").get(0).attr("value"));
//		map.put("bgresponse", allElements.select("input[name=bgresponse]").get(0).attr("value"));	//js_disabled
	}

	public static List<String> fetchAllA() throws Exception {
		List<String> list = new ArrayList<String>();
		int pageNum = 1;
		while(true) {
			List<String> res = fetchList(pageNum++);
			if(ComCollectionUtil.isCollectionEmpty(res)) {
				ComLogUtil.info("page ended at: " + pageNum);
				break;
			}
			list.addAll(res);
			res = null;
		}
		return list;
	}

	public static List<String> fetchDownloadpage(String url) throws Exception {
		HashMap<String, String> headerMap = new HashMap<String, String>();
		headerMap.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
		headerMap.put("Host", "www.dfidol.com");
		headerMap.put("Upgrade-Insecure-Requests", "1");
		headerMap.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36");
		headerMap.put("Accept-Encoding", "gzip, deflate");
		headerMap.put("Accept-Language", "en-US,en;q=0.9,zh;q=0.8,zh-CN;q=0.7,zh-TW;q=0.6");
//		headerMap.put("Host", "wwm");
//		headerMap.put("Host", "wwm");

		List<String> list = new ArrayList<String>();

        HttpGet httpGet = new HttpGet(url);
        StringResponse res = executeGet(httpGet, headerMap);
        int statusCode = res.getStatusCode();
        if(statusCode != 200) {
        	ComLogUtil.error("fetchDownloadPageFailed for link:" + url);
        	return list;
        }

        String resStr = res.getBody();
        httpGet.abort();
//        System.out.println(resStr);
        Document doc = Jsoup.parse(resStr);
//		Elements allElements = doc.getElementsByTag("input");
		Elements posts = doc.select(".single-post > a");
		int size = posts.size();

		for(int i = 0; i < size; i++) {
			Element post = posts.get(i);
			String link = post.attr("href");
			list.add(link);
			if(isDebug) System.out.println("extracted a download link:" + link);
		}
		return list;
	}

	public static List<String> fetchAllDownloadPage(File aLinkFile) throws Exception {
		List<String> list = new ArrayList<String>();
		String fileString = ComFileUtil.readFile2String(aLinkFile);
		String[] arr = fileString.split("\n", 9999);
		for(int i = 0; i < arr.length; i++) {
			String aLink = arr[i];
			ComLogUtil.info("about to fetch aLink: " + aLink);
			if(ComStrUtil.isBlankOrNull(aLink)) {
				ComLogUtil.info("skip a empty link");
			} else {
				List<String> downloadLinks = fetchDownloadpage(aLink);
				list.add(""); // add empty item to separate different file.
				list.addAll(downloadLinks);
			}
		}
		return list;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
//			fetchList(17);
//			List<String> allList = fetchAllA();
//			ComLogUtil.printCollection(allList, "");
//
//			ComFileUtil.writeString2File(allList.toString(), "d:\\logs\\a.txt", encode);


			List<String> allList = fetchAllDownloadPage(new File("d:\\logs\\allPages.txt"));
			ComLogUtil.printCollection(allList, "");

			ComFileUtil.writeString2File(allList.toString(), "d:\\logs\\allDownloads.txt", encode);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
