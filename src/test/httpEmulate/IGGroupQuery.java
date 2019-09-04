package test.httpEmulate;

import java.util.List;
import java.util.TreeSet;

/**
 * google group query interface
 * @author tiantc
 *
 */
public interface IGGroupQuery extends IGoogleLogin {
	
	/**
	 * retrieve ALL query result from googl group
	 * @param queryString string to query. e.g: "checking" will result in "${groupName} checking".
	 * if groupName = "group:my-langtest" then ->  "group:my-langtest checking"
	 * @return
	 */
	public List<String> query(String queryString) throws Exception;
	
	/**
	 * only query the subject field
	 * @param subject
	 * @return
	 * @throws Exception
	 */
	public TreeSet<String> querySubject(String subject) throws Exception;
	
}
