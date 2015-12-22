package dk.grixie.oauth2.app.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import dk.grixie.oauth2.app.R;
import dk.grixie.oauth2.app.model.MessageType;
import dk.grixie.oauth2.app.model.OAuth2WorkerProxy;

public class OAuth2Demo extends Activity {
    private final class MainHandler extends Handler {
        private MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.arg1 == MessageType.GET_ATTRIBUTES_RESPONSE.ordinal()) {
                EditText attributesText = (EditText) findViewById(R.id.oauth2_demo_get_user_attributes_response);

                if (message.obj instanceof String) {
                    attributesText.setText((String) message.obj);
                } else if (message.obj instanceof Exception) {
                    attributesText.setText(((Exception) message.obj).getLocalizedMessage());
                }
            } else if (message.arg1 == MessageType.GET_TIME_RESPONSE.ordinal()) {
                EditText timeText = (EditText) findViewById(R.id.oauth2_demo_get_time_response);

                if (message.obj instanceof String) {
                    timeText.setText((String) message.obj);
                } else if (message.obj instanceof Exception) {
                    timeText.setText(((Exception) message.obj).getLocalizedMessage());
                }
            } else if (message.arg1 == MessageType.GET_MESSAGE_RESPONSE.ordinal()) {
                EditText messageText = (EditText) findViewById(R.id.oauth2_demo_get_message_response);

                if (message.obj instanceof String) {
                    messageText.setText((String) message.obj);
                } else if (message.obj instanceof Exception) {
                    messageText.setText(((Exception) message.obj).getLocalizedMessage());
                }
            } else if (message.arg1 == MessageType.SET_MESSAGE_RESPONSE.ordinal()) {
                EditText messageText = (EditText) findViewById(R.id.oauth2_demo_get_message_response);

                if (message.obj instanceof String) {
                    messageText.setText((String) message.obj);
                } else if (message.obj instanceof Exception) {
                    messageText.setText(((Exception) message.obj).getLocalizedMessage());
                }
            } else if (message.arg1 == MessageType.OPEN_WEB_SOCKET_RESPONSE.ordinal()) {
                if (message.obj == null) {
                    Button openWebSocketButton = (Button) findViewById(R.id.oauth2_demo_open_web_socket_button);
                    openWebSocketButton.setEnabled(false);

                    Button echoButton = (Button) findViewById(R.id.oauth2_demo_echo_button);
                    echoButton.setEnabled(true);

                    Button closeWebSocketButton = (Button) findViewById(R.id.oauth2_demo_close_web_socket_button);
                    closeWebSocketButton.setEnabled(true);
                } else if (message.obj instanceof Exception) {
                    EditText echoText = (EditText) findViewById(R.id.oauth2_demo_echo_response);

                    echoText.setText(((Exception) message.obj).getLocalizedMessage());
                }
            } else if (message.arg1 == MessageType.CLOSE_WEB_SOCKET_RESPONSE.ordinal()) {
                Button openWebSocketButton = (Button) findViewById(R.id.oauth2_demo_open_web_socket_button);
                openWebSocketButton.setEnabled(true);

                Button echoButton = (Button) findViewById(R.id.oauth2_demo_echo_button);
                echoButton.setEnabled(false);

                Button closeWebSocketButton = (Button) findViewById(R.id.oauth2_demo_close_web_socket_button);
                closeWebSocketButton.setEnabled(false);

                if (message.obj instanceof Exception) {
                    EditText echoText = (EditText) findViewById(R.id.oauth2_demo_echo_response);

                    echoText.setText(((Exception) message.obj).getLocalizedMessage());
                }
            } else if (message.arg1 == MessageType.ECHO_RESPONSE.ordinal()) {
                EditText echoText = (EditText) findViewById(R.id.oauth2_demo_echo_response);

                if (message.obj instanceof String) {
                    echoText.setText((String) message.obj);
                } else if (message.obj instanceof Exception) {
                    echoText.setText(((Exception) message.obj).getLocalizedMessage());
                }
            }

        }
    }

    private Messenger messenger = null;
    private OAuth2WorkerProxy proxy = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        proxy = new OAuth2WorkerProxy(getApplicationContext());

//        startService(new Intent(this, OAuth2Service.class));

        messenger = new Messenger(new MainHandler(getMainLooper()));

        setContentView(R.layout.oauth2_demo);

        Button button = (Button) findViewById(R.id.oauth2_demo_grant_access_button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message message = Message.obtain();

                message.arg1 = MessageType.AUTHORIZATION_CODE_GRANT_REQUEST.ordinal();

                try {
                    proxy.getWorkerMessenger().send(message);
                } catch (RemoteException e) {
                    Log.e(this.getClass().getName(), e.toString());
                }
            }
        });

        Button getAttributesButton = (Button) findViewById(R.id.oauth2_demo_get_user_attributes_button);

        getAttributesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message message = Message.obtain();

                message.arg1 = MessageType.GET_ATTRIBUTES_REQUEST.ordinal();
                message.replyTo = messenger;

                try {
                    proxy.getWorkerMessenger().send(message);
                } catch (RemoteException e) {
                    Log.e(this.getClass().getName(), e.toString());
                }
            }
        });

        Button getTimeButton = (Button) findViewById(R.id.oauth2_demo_get_time_button);

        getTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message message = Message.obtain();

                message.arg1 = MessageType.GET_TIME_REQUEST.ordinal();
                message.replyTo = messenger;

                try {
                    proxy.getWorkerMessenger().send(message);
                } catch (RemoteException e) {
                    Log.e(this.getClass().getName(), e.toString());
                }
            }
        });

        Button getMessageButton = (Button) findViewById(R.id.oauth2_demo_get_message_button);

        getMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message message = Message.obtain();

                message.arg1 = MessageType.GET_MESSAGE_REQUEST.ordinal();
                message.replyTo = messenger;

                try {
                    proxy.getWorkerMessenger().send(message);
                } catch (RemoteException e) {
                    Log.e(this.getClass().getName(), e.toString());
                }
            }
        });

        final EditText setMessageText = (EditText) findViewById(R.id.oauth2_demo_set_message_response);
        Button setMessageButton = (Button) findViewById(R.id.oauth2_demo_set_message_button);

        setMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message message = Message.obtain();

                message.arg1 = MessageType.SET_MESSAGE_REQUEST.ordinal();
                message.obj = setMessageText.getText().toString();
                message.replyTo = messenger;

                try {
                    proxy.getWorkerMessenger().send(message);
                } catch (RemoteException e) {
                    Log.e(this.getClass().getName(), e.toString());
                }
            }
        });

        Button openWebsocketButton = (Button) findViewById(R.id.oauth2_demo_open_web_socket_button);
        openWebsocketButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message message = Message.obtain();

                message.arg1 = MessageType.OPEN_WEB_SOCKET_REQUEST.ordinal();
                message.replyTo = messenger;

                try {
                    proxy.getWorkerMessenger().send(message);
                } catch (RemoteException e) {
                    Log.e(this.getClass().getName(), e.toString());
                }
            }
        });

        Button closeWebsocketButton = (Button) findViewById(R.id.oauth2_demo_close_web_socket_button);
        closeWebsocketButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message message = Message.obtain();

                message.arg1 = MessageType.CLOSE_WEB_SOCKET_REQUEST.ordinal();
                message.replyTo = messenger;

                try {
                    proxy.getWorkerMessenger().send(message);
                } catch (RemoteException e) {
                    Log.e(this.getClass().getName(), e.toString());
                }
            }
        });

        final EditText echoMessageText = (EditText) findViewById(R.id.oauth2_demo_echo_request);
        Button echoMessageButton = (Button) findViewById(R.id.oauth2_demo_echo_button);

        echoMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message message = Message.obtain();

                message.arg1 = MessageType.ECHO_REQUEST.ordinal();
                message.obj = echoMessageText.getText().toString();
                message.replyTo = messenger;

                try {
                    proxy.getWorkerMessenger().send(message);
                } catch (RemoteException e) {
                    Log.e(this.getClass().getName(), e.toString());
                }
            }
        });
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
