package com.ascendcorp.exam.service;

import com.ascendcorp.exam.model.InquiryServiceResultDTO;
import com.ascendcorp.exam.model.TransferResponse;
import com.ascendcorp.exam.proxy.BankProxyGateway;
import com.ascendcorp.exam.requirepolicy.ErrorDefinition;
import com.ascendcorp.exam.requirepolicy.RequirePolicy;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


import javax.xml.ws.WebServiceException;
import java.util.Date;

public class InquiryService {

    @Autowired
    private BankProxyGateway bankProxyGateway;

    final static Logger log = Logger.getLogger(InquiryService.class);

    @Test
    public void test(){
        InquiryServiceResultDTO a = inquiry(null,new Date(),null,null,null,null,-1,
                null,null,null,null);
    }

    public InquiryServiceResultDTO inquiry(String transactionId,
                                           Date tranDateTime,
                                           String channel,
                                           String locationCode,
                                           String bankCode,
                                           String bankNumber,
                                           double amount,
                                           String reference1,
                                           String reference2,
                                           String firstName,
                                           String lastName)
    {
        InquiryServiceResultDTO respDTO = null;
        try
        {
            log.info("validate request parameters.");
            if(transactionId == null) {
                log.info(RequirePolicy.TRANS_ID);
                throw new NullPointerException(RequirePolicy.TRANS_ID);
            }
            if(tranDateTime == null) {
                log.info(RequirePolicy.DATE_TIME);
                throw new NullPointerException(RequirePolicy.DATE_TIME);
            }
            if(channel == null) {
                log.info(RequirePolicy.CHANNEL);
                throw new NullPointerException(RequirePolicy.CHANNEL);
            }
            if(bankCode == null || bankCode.isEmpty()) {
                log.info(RequirePolicy.BANK_CODE);
                throw new NullPointerException(RequirePolicy.BANK_CODE);
            }
            if(bankNumber == null || bankNumber.isEmpty()) {
                log.info(RequirePolicy.BANK_NUMBER);
                throw new NullPointerException(RequirePolicy.BANK_NUMBER);
            }
            if(amount <= 0) {
                log.info(RequirePolicy.AMOUNT);
                throw new NullPointerException(RequirePolicy.AMOUNT);
            }

            log.info("call bank web service");
            TransferResponse response = bankProxyGateway.requestTransfer(transactionId, tranDateTime, channel,
                    bankCode, bankNumber, amount, reference1, reference2);

            log.info("check bank response code");
            if(response != null) //New
            {
                log.debug("found response code");
                respDTO = new InquiryServiceResultDTO();

                respDTO.setRef_no1(response.getReferenceCode1());
                respDTO.setRef_no2(response.getReferenceCode2());
                respDTO.setAmount(response.getBalance());
                respDTO.setTranID(response.getBankTransactionID());

                String replyDesc = response.getDescription();
                String respDesc[] = null;
                if(replyDesc != null && !replyDesc.isEmpty()){
                    respDesc = replyDesc.split(":");
                }

                if("approved".equalsIgnoreCase(response.getResponseCode()))
                {
                    // bank response code = approved
                    respDTO.setReasonCode("200");
                    respDTO.setReasonDesc(replyDesc);
                    respDTO.setAccountName(replyDesc);

                }else if("invalid_data".equalsIgnoreCase(response.getResponseCode()))
                {
                    // bank response code = invalid_data
                    if(respDesc != null && respDesc.length >= 3)
                    {
                        // bank description full format
                        respDTO.setReasonCode(respDesc[1]);
                        respDTO.setReasonDesc(respDesc[2]);
                    }else
                    {
                        // bank description short format
                        respDTO.setReasonCode("400");
                        respDTO.setReasonDesc(ErrorDefinition.INVALID_DATA);
                    }

                }else if("transaction_error".equalsIgnoreCase(response.getResponseCode()))
                {
                    // bank response code = transaction_error
                    if(respDesc != null && respDesc.length >= 2)
                    {
                        log.info("Case Inquiry Error Code Format Now Will Get From [0] and [1] first");
                        String subIdx1 = respDesc[0];
                        String subIdx2 = respDesc[1];
                        log.info("index[0] : "+subIdx1 + " index[1] is >> "+subIdx2);
                        if("98".equalsIgnoreCase(subIdx1))
                        {
                            // bank code 98
                            respDTO.setReasonCode(subIdx1);
                            respDTO.setReasonDesc(subIdx2);
                        }else
                        {
                            log.info("case error is not 98 code");
                            if(respDesc.length >= 3)
                            {
                                // bank description full format
                                String subIdx3 = respDesc[2];
                                log.info("index[0] : "+subIdx3);
                                respDTO.setReasonCode(subIdx2);
                                respDTO.setReasonDesc(subIdx3);
                            }else
                            {
                                // bank description short format
                                respDTO.setReasonCode(subIdx1);
                                respDTO.setReasonDesc(subIdx2);
                            }
                        }
                    }else
                    {
                        // bank description incorrect format
                        respDTO.setReasonCode("500");
                        respDTO.setReasonDesc(ErrorDefinition.TRANS_ERROR);
                    }

                }else if("unknown".equalsIgnoreCase(response.getResponseCode()))
                {
                    if(respDesc != null && respDesc.length >= 2)
                    {
                        // bank description full format
                        respDTO.setReasonCode(respDesc[0]);
                        respDTO.setReasonDesc(respDesc[1]);
                        if(respDTO.getReasonDesc() == null || respDTO.getReasonDesc().trim().length() == 0)
                        {
                            respDTO.setReasonDesc(ErrorDefinition.INVALID_DATA);
                        }
                    }else
                    {
                        // bank description short format
                        respDTO.setReasonCode("501");
                        respDTO.setReasonDesc(ErrorDefinition.INVALID_DATA);
                    }

                }else
                    // bank code not support
                    throw new Exception(ErrorDefinition.UNKNOWN_CODE);
            }else
                // no resport from bank
                throw new Exception(ErrorDefinition.UNABLE_INQUIRY);
        }catch(NullPointerException ne)
        {
            if(respDTO == null)
            {
                respDTO = new InquiryServiceResultDTO();
                respDTO.setReasonCode("500");
                respDTO.setReasonDesc(ErrorDefinition.INVALID_DATA);
            }
        }catch(WebServiceException r)
        {
            // handle error from bank web service
            String faultString = r.getMessage();
            if(respDTO == null)
            {
                respDTO = new InquiryServiceResultDTO();
                if(faultString != null && (faultString.indexOf("java.net.SocketTimeoutException") > -1
                        || faultString.indexOf("Connection timed out") > -1 ))
                {
                    // bank timeout
                    respDTO.setReasonCode("503");
                    respDTO.setReasonDesc(ErrorDefinition.TIMEOUT);
                }else
                {
                    // bank general error
                    respDTO.setReasonCode("504");
                    respDTO.setReasonDesc(ErrorDefinition.APP_ERROR);
                }
            }
        }
        catch(Exception e)
        {
            log.error("inquiry exception", e);
            if(respDTO == null || (respDTO != null && respDTO.getReasonCode() == null))
            {
                respDTO = new InquiryServiceResultDTO();
                respDTO.setReasonCode("504");
                respDTO.setReasonDesc(ErrorDefinition.APP_ERROR);
            }
        }
        return respDTO;
    }
}
