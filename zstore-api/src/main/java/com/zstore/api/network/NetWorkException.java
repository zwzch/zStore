package com.zstore.api.network;

/**
 * @author baidu
 */
public class NetWorkException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private Code code;
    private String msg;

    public enum Code {
        /**
         * 错误列表
         */
        PATH_NOT_FOUND (1001),
        METHOD_NOT_SUPPORT (1002);;

        private int num;

        Code(Integer num) {
            this.num = num;
        }

        public Integer getNum() {
            return num;
        }

    }

    public NetWorkException(Code code, String msg) {
        super(code + ": " + msg);
        this.code = code;
        this.msg = msg;
    }

    public Code getCode() {
        return code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
