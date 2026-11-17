package pk.qist.device.auth;
oneway interface ICustomerSigninCallback {
    void onResult(boolean approved, String error);
}
