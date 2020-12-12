package br.com.webuyer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * dados sobre a lib da apache:
 * <p>
 * https://hc.apache.org/httpcomponents-client-5.0.x/quickstart.html
 * <p>
 * https://mkyong.com/java/apache-httpclient-examples/
 */
public class MainActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<HashMap> conteudo;
    //altere segundo o ip local da sua máquina
    String baseAPI = "http://192.168.56.1:8080/fornecedor?sort=nome";
    //url preenchida quando selecionamos algum registro na listagem
    String urlSelf = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.lista);
    }

    /**
     * método que atualiza os componentes gráficos
     */
    @Override
    protected void onResume() {
        super.onResume();
        final EditText edtnome = findViewById(R.id.edtNome);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    CloseableHttpClient httpclient = HttpClients.createDefault();
                    HttpGet httpGet = new HttpGet(baseAPI + "");
                    CloseableHttpResponse response1 = httpclient.execute(httpGet);
                    System.out.println(response1.getCode() + " " + response1.getReasonPhrase());
                    HttpEntity entity = response1.getEntity();

                    if (entity != null) {
                        // retorna uma String
                        String result = EntityUtils.toString(entity);
                        //transforma a String em um objeto json
                        JSONObject jsonObject = new JSONObject(result);
                        // recupera o array JSON de pessoas
                        JSONArray jsonArray = jsonObject.getJSONObject("_embedded").getJSONArray("pessoa");

                        if (jsonArray != JSONObject.NULL) {
                            // transforma a String em formato JSON num List Java
                            conteudo = (ArrayList) Util.toList(jsonArray);

                            /**
                             * atualiza a lista gráfica
                             */
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ArrayAdapter<HashMap> adapter = new ArrayAdapter<HashMap>(getBaseContext(), android.R.layout.simple_list_item_1, conteudo) {
                                        /**
                                         * reimplemento o getView para
                                         * mostrar uma propriedade específica
                                         *
                                         * @param position
                                         * @param convertView
                                         * @param parent
                                         * @return
                                         */
                                        @NonNull
                                        @Override
                                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                                            TextView textView = (TextView) super.getView(position, convertView, parent);
                                            //varia a cor da fonte entre vermelho e azul
                                            if (position % 2 == 0)
                                                textView.setTextColor(Color.BLUE);
                                            else
                                                textView.setTextColor(Color.RED);

                                            textView.setText("" + conteudo.get(position).get("nome"));
                                            return textView;
                                        }
                                    };
                                    listView.setAdapter(adapter);
                                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                        @Override
                                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                            //ao selecionar um registro da lista mostra o nome e armazena a url de alteração/exclusão
                                            Toast.makeText(getBaseContext(), "" + conteudo.get(i), Toast.LENGTH_SHORT).show();
                                            urlSelf = "" + ((HashMap) ((HashMap) conteudo.get(i).get("_links")).get("self")).get("href");
                                            edtnome.setText("" + conteudo.get(i).get("nome"));
                                        }
                                    });
                                }
                            });
                        }
                    }
                } catch (JSONException | IOException | ParseException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void salvar(View v) {
        final EditText edtnome = findViewById(R.id.edtNome);

        if (urlSelf == null) {
            //se não tiver url de alteração então é um registro novo --> vai inserir
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        CloseableHttpClient httpclient = HttpClients.createDefault();
                        //usa o POST
                        HttpPost httpPost = new HttpPost(baseAPI);
                        //prepara os parametros para envio
                        StringEntity params = new StringEntity("{\"nome\":\"" + edtnome.getText() + "\"}");
                        httpPost.addHeader("content-type", "application/json");
                        httpPost.setEntity(params);
                        //executa o request
                        CloseableHttpResponse response2 = httpclient.execute(httpPost);
                        // recupera o código da resposta
                        if (response2.getCode() == 200 || response2.getCode() == 201 || response2.getCode() == 202 || response2.getCode() == 204) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //atualiza a lista e exibe uma mensagem
                                    Toast.makeText(getBaseContext(), "Inserido com sucesso", Toast.LENGTH_LONG).show();
                                    urlSelf = null;
                                    edtnome.setText("");
                                    onResume();
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } else { //alteração
            //nesse caso existe uma url de alteração selecionada
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        CloseableHttpClient httpclient = HttpClients.createDefault();
                        //usa o método PATCH
                        HttpPatch httpPatch = new HttpPatch(urlSelf);
                        //prepara os parâmetros para envio
                        StringEntity params = new StringEntity("{\"nome\":\"" + edtnome.getText() + "\"}");
                        httpPatch.addHeader("content-type", "application/json");
                        httpPatch.setEntity(params);
                        //executa o envio da requisição
                        CloseableHttpResponse response2 = httpclient.execute(httpPatch);
                        // recupera o código da resposta
                        if (response2.getCode() == 200 || response2.getCode() == 201 || response2.getCode() == 202 || response2.getCode() == 204) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //atualiza a lista e exibe uma mensagem
                                    Toast.makeText(getBaseContext(), "Alterado com sucesso", Toast.LENGTH_LONG).show();
                                    urlSelf = null;
                                    edtnome.setText("");
                                    onResume();
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }
    }

    public void excluir(View v) {
        final EditText edtnome = findViewById(R.id.edtNome);
        if (urlSelf != null) { //exclusão
            //se tiver uma url selecionada então pode excluir
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        CloseableHttpClient httpclient = HttpClients.createDefault();
                        //usa o método DELETE
                        HttpDelete httpDelete = new HttpDelete(urlSelf);
                        CloseableHttpResponse response2 = httpclient.execute(httpDelete);
                        // recupera o código da resposta
                        if (response2.getCode() == 200 || response2.getCode() == 201 || response2.getCode() == 202 || response2.getCode() == 204) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //atualiza a lista e exibe uma mensagem
                                    Toast.makeText(getBaseContext(), "Excluído com sucesso", Toast.LENGTH_LONG).show();
                                    urlSelf = null;
                                    edtnome.setText("");
                                    onResume();
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }
    }
}