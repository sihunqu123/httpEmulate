package test.httpEmulate.Model;

public class StringResponse {

	int statusCode;

	String body;

	public StringResponse(int statusCode, String body) {
		super();
		this.statusCode = statusCode;
		this.body = body;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}




}
