package com.studynote.notes.exception;

/**
 * 权限不足（未登录 / 非管理员）异常。
 */
public class NoPermissionException extends RuntimeException {

    public NoPermissionException(String message) {
        super(message);
    }
}
