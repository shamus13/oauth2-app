package dk.grixie.oauth2.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import dk.grixie.oauth2.app.R;
import dk.grixie.oauth2.app.model.MessageType;
import dk.grixie.oauth2.app.model.OAuth2WorkerProxy;
import dk.grixie.oauth2.app.oauth2.exception.OAuth2Exception;

public class ReceiveToken extends Activity {
    private final class ReceiveTokenHandler extends Handler {
        private ReceiveTokenHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.arg1 == MessageType.ACCESS_TOKEN_ACQUIRED.ordinal()) {
                final TextView status = (TextView) findViewById(R.id.receive_token_status);
                final EditText error = (EditText) findViewById(R.id.receive_token_error_description);
                final Button button = (Button) findViewById(R.id.receive_token_error_uri_button);

                status.setText(R.string.receive_token_token_status_ok);
                error.setText("");
                button.setEnabled(false);
            } else if (message.arg1 == MessageType.ACCESS_TOKEN_NOT_ACQUIRED.ordinal()) {
                final TextView status = (TextView) findViewById(R.id.receive_token_status);
                final EditText error = (EditText) findViewById(R.id.receive_token_error_description);
                final Button button = (Button) findViewById(R.id.receive_token_error_uri_button);

                if (message.obj instanceof OAuth2Exception) {
                    final OAuth2Exception oAuth2Exception = (OAuth2Exception) message.obj;

                    status.setText(oAuth2Exception.getMessage());
                    error.setText(oAuth2Exception.getDescription());

                    if (oAuth2Exception.getUri() != null) {
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent("android.intent.action.VIEW",
                                        Uri.parse(oAuth2Exception.getUri().toString()));

                                startActivity(intent);
                            }
                        });
                        button.setEnabled(true);
                    }

                } else if (message.obj instanceof Exception) {
                    Exception e = (Exception) message.obj;

                    status.setText("Error");
                    error.setText(e.getLocalizedMessage());
                }
            }
        }
    }

    private OAuth2WorkerProxy proxy = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        proxy = new OAuth2WorkerProxy(getApplicationContext());

//        startService(new Intent(this, OAuth2Service.class));

        Intent intent = getIntent();

        if (intent != null && intent.getData() != null) {
            setContentView(R.layout.receive_token);

            final TextView status = (TextView) findViewById(R.id.receive_token_status);
            final EditText error = (EditText) findViewById(R.id.receive_token_error_description);
            final Button button = (Button) findViewById(R.id.receive_token_error_uri_button);

            status.setText("Working");
            error.setText("");
            button.setEnabled(false);

            Message message = Message.obtain();

            message.arg1 = MessageType.AUTHORIZATION_CODE_GRANT_RESPONSE.ordinal();
            message.replyTo = new Messenger(new ReceiveTokenHandler(getMainLooper()));
            message.obj = intent.getData();

            try {
                proxy.getWorkerMessenger().send(message);
            } catch (RemoteException e) {
                //ignore this problem
            }

        } else {
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        proxy.release();
        proxy = null;
    }
}
