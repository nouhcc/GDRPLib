package lonodev.gdprlib.com.gdprlib;

/**
 * Created by nouhcc on 07/06/2018.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.consent.AdProvider;
import com.google.ads.consent.ConsentInfoUpdateListener;
import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;

import java.util.List;

/**
 * Created by nouhcc on 04/06/2018.
 */

public class GDPRLib {

    public static class Builder {
        private Context context;
        private LayoutInflater inflater;
        private String privacyURL;
        private String publisherId;
        Consent_callback consent_callback;
        private  int RImage =0;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder addLayoutInflater(LayoutInflater inflater) {
            this.inflater = inflater;
            return this;
        }
        public Builder addCallBack(Consent_callback consent_callback) {
            this.consent_callback = consent_callback;

            return this;
        }

        public Builder addPrivacyPolicy(String privacyURL) {
            this.privacyURL = privacyURL;
            return this;
        }

        public Builder addPublisherId(String publisherId) {
            this.publisherId = publisherId;
            return this;
        }
        public Builder addDrowableId(int DrowableId) {
            this.RImage = DrowableId;
            return this;
        }

        public GDPRLib build() {
            GDPRLib GDRPlib = new GDPRLib(this.context, this.inflater,this.consent_callback, this.privacyURL,this.publisherId ,this.RImage);
            return GDRPlib;
        }
    }

    public static boolean mShowNonPersonalizedAdRequests = false;
    private AlertDialog mEuDialog;
    public String ID_Device = "YOUR-DEVICE-ID";

    public static Context context=null;
    public LayoutInflater inflater=null;
    private String publisherId;
    private String PRIVACY_URL = "";
    private Consent_callback callback;
    private SharedPreferences custom_prefence;
    private SharedPreferences default_prefence;
    public static boolean isFromEU = false;
    public int RImage =0;

    public GDPRLib(Context context,LayoutInflater inflater,Consent_callback consent_callback,String PRIVACY_URL,String publisherId,int DrowableId){
        this.context     = context;
        this.inflater    = inflater;
        this.callback    = consent_callback;
        this.publisherId = publisherId;
        this.PRIVACY_URL = PRIVACY_URL;
        this.RImage      = DrowableId;
        custom_prefence  = context.getSharedPreferences("MAIN_PREF", Context.MODE_PRIVATE);
        default_prefence = PreferenceManager.getDefaultSharedPreferences(context);

    }

    public static boolean result=false;
    // https://developers.google.com/admob/android/eu-consent
    public void checkConsentStatus(Context context){

        ConsentInformation consentInformation = ConsentInformation.getInstance(context);
        ConsentInformation.getInstance(context).addTestDevice(ID_Device); // enter your device id, if you need it for testing

        String[] publisherIds = {publisherId};// enter your admob pub-id
        consentInformation.requestConsentInfoUpdate(publisherIds, new ConsentInfoUpdateListener() {
            @Override
            public void onConsentInfoUpdated(ConsentStatus consentStatus) {
                Log.d("nouh","User's consent status successfully updated: " +consentStatus);

                if (ConsentInformation.getInstance(GDPRLib.context).isRequestLocationInEeaOrUnknown()){
                    Log.d("nouh","User is from EU");
                    // If the returned ConsentStatus is UNKNOWN, collect user's consent.
                    if (consentStatus == ConsentStatus.UNKNOWN) {
                        isFromEU = true;
                        showMyConsentDialog(false);
                    }
                    else if (consentStatus == ConsentStatus.NON_PERSONALIZED) {
                        isFromEU = true;
                        callback.OnDialogClose();
                    }
                    else if (consentStatus == ConsentStatus.PERSONALIZED) {
                        isFromEU = true;
                        callback.OnDialogClose();
                    }


                } else {
                    Log.d("nouh","User is NOT from EU");
                    // we don't have to do anything
                    setYourPerfernce(GDPRLib.context ,false);
                    callback.OnDialogClose();
                    isFromEU = false;

                }

            }

            @Override
            public void onFailedToUpdateConsentInfo(String errorDescription) {
                Log.d("nouh","User's consent status failed to update: " +errorDescription);
                setYourPerfernce(GDPRLib.context ,false);
                callback.OnDialogClose();
            }
        });
    }


    public void showMyConsentDialog(boolean showCancel) {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context, R.style.AppThemeDialog);
        LayoutInflater inflater = this.inflater;
        View eu_consent_dialog = inflater.inflate(R.layout.eu_consent, null);

        alertDialog.setView(eu_consent_dialog).setCancelable(false);

        if (showCancel) alertDialog.setPositiveButton(R.string.eu_dialog_close, null);

        mEuDialog = alertDialog.create();
        try {
            mEuDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }catch (NullPointerException e){
        }
        mEuDialog.show();

        ImageView eu_consent_Logo = eu_consent_dialog.findViewById(R.id.imageView3);
        if(!(RImage==0))
        eu_consent_Logo.setImageResource(RImage);
        else
            eu_consent_Logo.setImageResource(R.mipmap.ic_launcher);
        Button btn_eu_consent_yes = eu_consent_dialog.findViewById(R.id.btn_eu_consent_yes);
        Button btn_eu_consent_no = eu_consent_dialog.findViewById(R.id.btn_eu_consent_no);
        btn_eu_consent_yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEuDialog.cancel();
                Toast.makeText( context,context.getString(R.string.eu_thank_you),Toast.LENGTH_SHORT).show();

                ConsentInformation.getInstance(context).setConsentStatus(ConsentStatus.PERSONALIZED);
                mShowNonPersonalizedAdRequests = false;
                setYourPerfernce(context ,mShowNonPersonalizedAdRequests);
                callback.OnDialogClose();
            }
        });
        btn_eu_consent_no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEuDialog.cancel();
                Toast.makeText( context,context.getString(R.string.eu_thank_you), Toast.LENGTH_SHORT).show();
                ConsentInformation.getInstance(context).setConsentStatus(ConsentStatus.NON_PERSONALIZED);
                mShowNonPersonalizedAdRequests = true;
                setYourPerfernce(context ,mShowNonPersonalizedAdRequests);
                callback.OnDialogClose();
            }
        });

        TextView tv_eu_learn_more = eu_consent_dialog.findViewById(R.id.tv_eu_learn_more);
        tv_eu_learn_more.setPaintFlags(tv_eu_learn_more.getPaintFlags() |   Paint.UNDERLINE_TEXT_FLAG);
        tv_eu_learn_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                euMoreInfoDialog();
            }
        });
    }
    public void euMoreInfoDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppThemeDialog);

        ScrollView sv = new ScrollView(context);
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(40, 20, 40, 20);

        TextView tv_my_privacy_policy = new TextView(context);
        String link = context.getResources().getString(R.string.eu_Text2_learn_more)+" <a href="+PRIVACY_URL+">"+context.getResources().getString(R.string.app_name)+"</a>  <br/>"+context.getResources().getString(R.string.eu_Text3_learn_more)+" :";
        tv_my_privacy_policy.setText(Html.fromHtml(link));
        tv_my_privacy_policy.setMovementMethod(LinkMovementMethod.getInstance());
        tv_my_privacy_policy.setTextColor(context.getResources().getColor(R.color.black));
        ll.addView(tv_my_privacy_policy, params);

        TextView tv_google_partners = new TextView(context);
        tv_google_partners.setText(R.string.eu_google_partners);
        tv_google_partners.setPadding(40,40,40,20);
        ll.addView(tv_google_partners);

        List<AdProvider> adProviders = ConsentInformation.getInstance(context).getAdProviders();
        for (AdProvider adProvider : adProviders) {
            //log("adProvider: " +adProvider.getName()+ " " +adProvider.getPrivacyPolicyUrlString());
            link = "<a href="+adProvider.getPrivacyPolicyUrlString()+">"+adProvider.getName()+"</a>";
            TextView tv_adprovider = new TextView(context);
            tv_adprovider.setText(Html.fromHtml(link));
            tv_adprovider.setMovementMethod(LinkMovementMethod.getInstance());
            tv_adprovider.setTextColor(context.getResources().getColor(R.color.lonodev_colorProviders));
            ll.addView(tv_adprovider, params);
        }
        sv.addView(ll);

        builder.setTitle(R.string.eu_privacy_policy)
                .setView(sv)
                .setPositiveButton(R.string.eu_dialog_close, null);

        final AlertDialog createDialog = builder.create();
        createDialog.show();

    }

    public AlertDialog getDialog(){
        return mEuDialog;
    }

    public static AdRequest getAdRequest(Context context) {
        return  !getYourPerfernce(context)?
                (new com.google.android.gms.ads.AdRequest.Builder()).build()
                :(new com.google.android.gms.ads.AdRequest.Builder())
                .addNetworkExtrasBundle(AdMobAdapter.class, getNonPersonalizedAdsBundle()).build();
    }

    private static Bundle getNonPersonalizedAdsBundle() {
        Bundle extras = new Bundle();
        extras.putString("npa", "1");
        return extras;
    }



    public static void setYourPerfernce(Context context,boolean name) {
        SharedPreferences default_prefence = PreferenceManager.getDefaultSharedPreferences(context);
        Log.d("nouh","No Personnalzed ADS "+default_prefence.getBoolean("GDPR_custom_providers",false));
        default_prefence.edit().putBoolean("GDPR_custom_providers", name).apply();
    }
    public static Boolean getYourPerfernce(Context context) {
        SharedPreferences default_prefence = PreferenceManager.getDefaultSharedPreferences(context);
        Log.d("nouh","No Personnalzed ADS "+default_prefence.getBoolean("GDPR_custom_providers",false));
        return default_prefence.getBoolean("GDPR_custom_providers",false);
    }
}
