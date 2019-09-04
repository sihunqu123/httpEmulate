package test.httpEmulate.impl;

import java.io.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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

import test.httpEmulate.IGGroupQuery;
import test.httpEmulate.IGoogleLogin;

public class GGroupQueryerImpl extends GoogleLoginImpl implements IGGroupQuery {
	
    private static String GGroupName;
    private static String[] highFreTerms;
    private File outputFile;
    
	
	
    public GGroupQueryerImpl() throws Exception {
		super();
		init();
    }

    
    private void init() throws Exception {
    	Properties p = new Properties();
        GGroupName = configprops.getProperty("ggroupname");
        outputFile = new File(configprops.getProperty("queryResultOutput"));
        highFreTerms = readFile2String(new File(configprops.getProperty("highFrequenceTermsLoc"))).split("(\r\n|\n\r|\r|\n)+");
        myGroup();
        if(isDebug) System.out.println("------------------------------------------initOver----------------------------------------------");
    }
    
	
	/**
	 *  launch googleForm hompage
	 */
	private void myGroup() throws Exception {
        HashMap<String, String> headerMap = new HashMap<String, String>();
        //headerMap.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        
        HttpGet httpGet = new HttpGet("https://groups.google.com/forum/");
        
        String resStr = this.executeGet(httpGet, headerMap);
        
        if(isDebug) writeString2File(resStr, new File(logFilePath + "/myGroup.txt"));
        
        //store some info for later http request use.
        String tmpStr1 = getMatchedString(resStr, "(?<=src=\"/forum/)(\\w|-){30,34}(?=\\.cache\\.js)");
        String tmpStr2 = getMatchedString(resStr, "(?<=\"xsrf-token\":\")(\\w|-){34}:\\d{13}(?=\",)");
        map.put("X-GWT-Permutation", tmpStr1);
        map.put("xsrf-token", tmpStr2);
        if(tmpStr1 == null) {
        	System.err.println("X-GWT-Permutation:" + tmpStr1);
        }
        if(tmpStr2 == null) {
        	System.err.println("xsrf-token:" + tmpStr2);
        }
	}
	
	/**
	 * generate a result json file from input high frequence terms.
	 */
	public void generateQueryResult() {
		FileWriter fw = null;
		try {
			int length = highFreTerms.length;
			int i = 0;
			fw = new FileWriter(outputFile, true);
			StringBuilder sb = new StringBuilder();
			JSONObject jo = new JSONObject();
			for(i = 0; i < length; i++) {
				System.out.println("start to query:" + highFreTerms[i]);
				jo.put(highFreTerms[i], query(highFreTerms[i]));
				//System.out.println(jo.toString());
				sb.append(jo.toString());
				//fw.write(jo.toString());
				Thread.sleep(1000 * randGen.nextInt(3));
			}
			if(isDebug) System.out.println("res:" + sb);
			fw.write(jo.toString());
			fw.flush();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			 try {
				if(fw != null) {
					fw.close();
					fw = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			 
		}
		
	}
	
	/**
	 * do query once; we may need to search "next page". Search From google group.
	 * @param queryStr the term to query.
	 * @param pageMax the number of result that retrieve per page. default is 20
	 * @param currentPage start from 0
	 * @param is4Cnt true: Search From my group in google group; false:  Search From google group.
	 */
	private List<String> searchGGroupOnce(String queryStr, int pageMax, int currentPage, boolean is4Cnt) throws Exception {
        List<String> resList = null;
        HashMap<String, String>headerMap = new HashMap<String, String>();
		headerMap.put("Host", "groups.google.com");
		headerMap.put("Content-type", "text/x-gwt-rpc; charset=UTF-8");
		headerMap.put("X-GWT-Module-Base", "https://groups.google.com/forum/");
		headerMap.put("X-GWT-Permutation", map.get("X-GWT-Permutation"));
		headerMap.put("X-Groups-Group-Name", "my-langtest");
		
		String payloadStr = null;
		if(is4Cnt) {
			 payloadStr = new StringBuilder("7|3|13|https://groups.google.com/forum/|431EDF28DA7DB0F110D897C9D8624A20|5g|")
						.append(map.get("xsrf-token"))
						.append("|_|getMatchingMessages|5n|i|I|1u|5h|")
						//.append("group:").append(GGroupName).append(" ")
						.append(queryStr)
						.append("|").append(GGroupName)
						.append("|1|2|3|4|5|6|6|7|8|9|9|10|11|12|8|13|13|0|0|")
						.append(currentPage * pageMax).append('|').append(pageMax)
						.append("|10|5|0|")
						
						.toString();
		} else {
			payloadStr = new StringBuilder("7|3|12|https://groups.google.com/forum/|431EDF28DA7DB0F110D897C9D8624A20|5g|")
					.append(map.get("xsrf-token"))
					.append("|_|getMatchingMessages|5n|i|I|1u|5h|group:")
					.append(GGroupName).append(" ").append(queryStr)
					.append("|1|2|3|4|5|6|6|7|8|9|9|10|11|12|0|")
					.append(currentPage * pageMax).append('|').append(pageMax)
					.append("|0|0|")
					.toString();
		}
		
        
        if(isDebug) System.out.println(new StringBuilder("query:").append(queryStr)
				.append(", pageMax:").append(pageMax)
				.append(", currentPage:").append(currentPage)
        		);
        if(isDebug) System.out.println("payload:" + payloadStr);
        
        HttpPost httpPost = new HttpPost("https://groups.google.com/forum/fsearch?appversion=1&hl=en&authuser=0");
        
        String resStr = this.executePost(httpPost, headerMap, payloadStr.toString());
        
		resList = getResList(resStr);
		
		//if we still have next page;
		if(resList.size() >= pageMax
				//&& currentPage < 10 // we need to retrieve all results.
				) {
			resList.addAll(searchGGroupOnce(queryStr, pageMax, ++currentPage, is4Cnt));
		}
		return resList;
	}
	
	/**
	 * retrieve all query result by body
	 * @param queryString string to query the body field
	 * @return a list of result's subject
	 */
	@Override
	public List<String> query(String queryString) throws Exception {
		List<String> resList = null;
	        
        int currentPage = 0;	//start from 0
        int pageMax = 20;	//default 20
        
        resList = searchGGroupOnce(queryString, pageMax, currentPage++, false);
        if(isDebug) {
        	System.out.println("res:\n");
        	printIterator(resList, "");
        }
        return resList;
	}

	/**
	 * retrieve all query result by subject.
	 * @param queryString string to query the subject field
	 * @return a TreeSet of result's subject
	 */
	@Override
	public TreeSet<String> querySubject(String subject) throws Exception {
//		List<String> resList = null;
        
        int currentPage = 0;	//start from 0
        int pageMax = 5000;	//default 20

        //resList = searchGGroupOnce4Cnt("subject:" + subject, pageMax, currentPage++);
        
        TreeSet<String> treeSet = new TreeSet<String>(new Comparator<String>() {
        	@Override
        	public int compare(String o1, String o2) {
//        		int i1 = NewsEntity.getIndexFrmSubject(o1);
//        		int i2 = NewsEntity.getIndexFrmSubject(o2);
//        		if(i1 == i2) return 0;
//        		return i1 > i2 ? 1 : -1;
        		return 0;
        	}
		});
        //since those 2 method may return different result, so we union all of them.(Of course no duplicate result.)
        treeSet.addAll(searchGGroupOnce("subject:" + subject, pageMax, currentPage++, true));
        treeSet.addAll(searchGGroupOnce("subject:" + subject, pageMax, currentPage++, false));
        if(isDebug) {
        	System.out.println("res:\n");
            printIterator(treeSet, "");
        }
        
        return treeSet;
	}
	
	
	public static void main(String[] args) {
		GGroupQueryerImpl ggq;
		try {
			ggq = new GGroupQueryerImpl();
			//ggq.querySubject("ja");
			ggq.generateQueryResult();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
}