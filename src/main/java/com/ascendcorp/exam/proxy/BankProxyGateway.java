package com.ascendcorp.exam.proxy;

import com.ascendcorp.exam.model.TransferResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;


public class BankProxyGateway {

    public TransferResponse requestTransfer(
            String transactionId, Date tranDateTime, String channel,
            String bankCode, String bankNumber, double amount,
            String reference1, String reference2) {

        return new TransferResponse();
    }
}

