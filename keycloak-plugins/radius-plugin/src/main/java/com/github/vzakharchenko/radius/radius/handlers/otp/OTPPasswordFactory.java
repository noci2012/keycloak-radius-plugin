package com.github.vzakharchenko.radius.radius.handlers.otp;

import com.github.vzakharchenko.radius.radius.handlers.session.KeycloakSessionUtils;
import com.github.vzakharchenko.radius.radius.holder.IRadiusUserInfo;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.credential.OTPCredentialProviderFactory;
import org.keycloak.models.*;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.dto.OTPCredentialData;
import org.keycloak.models.credential.dto.OTPSecretData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.keycloak.models.credential.OTPCredentialModel.HOTP;
import static org.keycloak.models.credential.OTPCredentialModel.TOTP;

public class OTPPasswordFactory implements IOtpPasswordFactory {

    private final Map<String, IOTPPassword> otpPasswordFactories;

    public OTPPasswordFactory() {
        this.otpPasswordFactories = new HashMap<>();
        this.otpPasswordFactories.put(HOTP, new HotpPassword());
        this.otpPasswordFactories.put(TOTP, new TotpPassword());
    }

    private SubjectCredentialManager getCredentialStore(UserModel userModel) {
        return userModel.credentialManager();
    }

    private boolean isUserRequireOtp(UserModel userModel) {
        return userModel.getRequiredActionsStream()
            .anyMatch(action -> UserModel.RequiredAction.CONFIGURE_TOTP.name().equals(action));
    }

    private void initOTPPasswords(RealmModel realm,
                                  OtpPasswordInfo otpPasswordInfo,
                                  CredentialModel credential) {
        OTPCredentialModel otpCredentialModel = OTPCredentialModel
                .createFromCredentialModel(credential);
        OTPSecretData secretData = otpCredentialModel.getOTPSecretData();
        OTPCredentialData credentialData = otpCredentialModel.getOTPCredentialData();
        OTPPolicy policy = realm.getOTPPolicy();
        IOTPPassword otpPassword = otpPasswordFactories.get(credentialData.getSubType());
        if (otpPassword != null) {
            otpPasswordInfo.putAll(otpPassword.getOTPPasswords(
                    credentialData, policy, secretData, credential));
        }
    }

    private List<CredentialModel> filterCredentials(RealmModel realmModel,
                                                    UserModel userModel) {
        return getCredentialStore(userModel)
                .getStoredCredentialsByTypeStream(OTPCredentialModel.TYPE)
                .filter(credentialModel -> Objects
                        .equals(OTPCredentialModel.createFromCredentialModel(credentialModel)
                                .getOTPCredentialData().getSubType(), realmModel
                                .getOTPPolicy().getType())).collect(Collectors.toList());
    }

    @Override
    public OtpPasswordInfo getOTPs(KeycloakSession session) {
        IRadiusUserInfo radiusSessionInfo = KeycloakSessionUtils
                .getRadiusSessionInfo(session);
        UserModel userModel = radiusSessionInfo.getUserModel();
        RealmModel realm = radiusSessionInfo.getRealmModel();
        List<CredentialModel> credentials = filterCredentials(realm, userModel);
        OtpPasswordInfo otpPasswordInfo = new OtpPassword(isUserRequireOtp(userModel),
                radiusSessionInfo.getClientModel());
        for (CredentialModel credential : credentials) {
            initOTPPasswords(realm, otpPasswordInfo, credential);
        }
        return otpPasswordInfo;
    }

    @Override
    public void validOTP(KeycloakSession session, String password, String credId, String type) {
        IRadiusUserInfo radiusSessionInfo = KeycloakSessionUtils
                .getRadiusSessionInfo(session);
        UserModel userModel = radiusSessionInfo.getUserModel();
        RealmModel realm = radiusSessionInfo.getRealmModel();
        OTPCredentialProvider provider = (OTPCredentialProvider) session
                .getProvider(CredentialProvider.class,
                        OTPCredentialProviderFactory.PROVIDER_ID);
        provider.isValid(realm, userModel, new UserCredentialModel(credId, type, password));
    }

}
