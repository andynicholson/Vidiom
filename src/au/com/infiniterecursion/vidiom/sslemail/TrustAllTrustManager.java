package au.com.infiniterecursion.vidiom.sslemail;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import android.util.Log;

public class TrustAllTrustManager implements X509TrustManager {
	private static final String TAG = "VidiomTag-TrustAllTrustManager";

	public TrustAllTrustManager() {
	}

	public X509Certificate[] getAcceptedIssuers() {
		throw new UnsupportedOperationException();
	}

	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		throw new UnsupportedOperationException();
	}

	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {

		Log.d(TAG, " in checkServerTrusted! \n " + chain.toString() + " \n "
				+ authType);

	}
}
