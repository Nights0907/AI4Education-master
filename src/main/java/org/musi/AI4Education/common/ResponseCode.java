package org.musi.AI4Education.common;

import lombok.Getter;

@Getter
public enum ResponseCode
{
    ERROR(0, "ERROR"),
    SUCCESS(1, "SUCCESS"),
    NEED_LOGIN(10, "NEED_LOGIN"),
    ILLEGAL_ARGUMENT(2, "ILLEGAL_ARGUMENT");

    private final int code;
    private final String description;

    ResponseCode(int code, String description)
    {
        this.code = code;
        this.description = description;
    }

}
