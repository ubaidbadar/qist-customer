package pk.qist.device.auth;
import pk.qist.device.auth.ICustomerSigninCallback;
interface ICustomerSignin {
    void authenticate(String requestId, String nonce, ICustomerSigninCallback callback);
}
