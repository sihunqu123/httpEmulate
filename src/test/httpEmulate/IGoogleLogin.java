package test.httpEmulate;

/**
 * 
 * Google login interface
 * @author tiantc
 *
 */
public interface IGoogleLogin {
	
	public final static String encode = "utf-8";
	
//	public final static boolean isDebug = true;
	
	/**
	 * login to google group
	 * @return true -> login success; false -> login failed.
	 * @throws Exception 
	 */
	Boolean login();
	
	
	
}
