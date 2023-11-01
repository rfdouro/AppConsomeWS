package br.org.aluno.appconsomews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<HashMap> conteudo;
    //altere segundo a url da API
    String baseAPI = "http://rfdouro.vps-kinghost.net:8080/services/demos/pessoa";
    //objeto preenchido quando selecionamos algum registro na listagem
    HashMap selecionado = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.lista);
    }

    public Context getContext() {
        return this;
    }

    @Override
    protected void onResume() {
        super.onResume();

        final EditText edtnome = findViewById(R.id.edtNome);
        final EditText edtcpf = findViewById(R.id.edtCpf);
        final EditText edtnascimento = findViewById(R.id.edtNascimento);

        //atributo que será um diálogo exibido enquanto é feita a leitura no webservice
        ProgressDialog dialog = ProgressDialog.show(getContext(), "Aguarde",
                "Baixando informações, Por Favor Aguarde...");
        dialog.setCancelable(true);
        dialog.show();

        RequestQueue queue = Volley.newRequestQueue(this);

        // cria uma requisição json para listar
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, baseAPI, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonArray = response.getJSONArray("content");
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
                                        edtnome.setText("" + conteudo.get(i).get("nome"));
                                        edtcpf.setText("" + conteudo.get(i).get("cpf"));
                                        edtnascimento.setText("" + conteudo.get(i).get("nascimento"));
                                        selecionado = conteudo.get(i);
                                    }
                                });
                                dialog.dismiss();
                            }
                        });
                    }
                } catch (Exception ex) {

                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // trata o erro
            }
        });
        // adiciona a requisição na fila
        queue.add(jsonObjectRequest);
    }

    public void salvar(View v) {
        final EditText edtnome = findViewById(R.id.edtNome);
        final EditText edtcpf = findViewById(R.id.edtCpf);
        final EditText edtnascimento = findViewById(R.id.edtNascimento);

        if (selecionado == null) {
            //se não tiver selecionado então é um registro novo --> vai inserir
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        RequestQueue queue = Volley.newRequestQueue(getBaseContext());
                        //cria o objeto para receber os dados
                        selecionado = new HashMap();
                        //atribui o nome digitado à chave correspondente
                        selecionado.put("nome", "" + edtnome.getText());
                        selecionado.put("cpf", "" + edtcpf.getText());
                        selecionado.put("nascimento", "" + edtnascimento.getText());
                        //prepara os parâmetros para envio
                        JSONObject os = new JSONObject(selecionado);
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, baseAPI, os,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                //atualiza a lista e exibe uma mensagem
                                                Toast.makeText(getBaseContext(), "Inserido com sucesso", Toast.LENGTH_LONG).show();
                                                selecionado = null;
                                                edtnome.setText("");
                                                edtcpf.setText("");
                                                edtnascimento.setText("");
                                                onResume();
                                            }
                                        });
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                System.out.println(error.getMessage());
                            }
                        });
                        queue.add(jsonObjectRequest);
                    } catch (Exception e) {
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
                        RequestQueue queue = Volley.newRequestQueue(getBaseContext());
                        //atribui o nome digitado à chave correspondente
                        selecionado.put("nome", "" + edtnome.getText());
                        selecionado.put("cpf", "" + edtcpf.getText());
                        selecionado.put("nascimento", "" + edtnascimento.getText());
                        //prepara os parâmetros para envio
                        JSONObject os = new JSONObject(selecionado);
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PUT, baseAPI, os,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                //atualiza a lista e exibe uma mensagem
                                                Toast.makeText(getBaseContext(), "Alterado com sucesso", Toast.LENGTH_LONG).show();
                                                selecionado = null;
                                                edtnome.setText("");
                                                edtcpf.setText("");
                                                edtnascimento.setText("");
                                                onResume();
                                            }
                                        });
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                System.out.println(error.getMessage());
                            }
                        });
                        queue.add(jsonObjectRequest);

                        //httpPut.addHeader("content-type", "application/json");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public void excluir(View v) {
        final EditText edtnome = findViewById(R.id.edtNome);
        final EditText edtcpf = findViewById(R.id.edtCpf);
        final EditText edtnascimento = findViewById(R.id.edtNascimento);

        if (selecionado != null) { //exclusão
            //se tiver uma url selecionada então pode excluir
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        RequestQueue queue = Volley.newRequestQueue(getBaseContext());
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.DELETE, baseAPI + "/" + selecionado.get("id"), null,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                //atualiza a lista e exibe uma mensagem
                                                Toast.makeText(getBaseContext(), "Excluído com sucesso", Toast.LENGTH_LONG).show();
                                                selecionado = null;
                                                edtnome.setText("");
                                                edtcpf.setText("");
                                                edtnascimento.setText("");
                                                onResume();
                                            }
                                        });
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                System.out.println(error.getMessage());
                            }
                        });
                        queue.add(jsonObjectRequest);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }
    }

    public void cancelar(View v) {
        final EditText edtnome = findViewById(R.id.edtNome);
        final EditText edtcpf = findViewById(R.id.edtCpf);
        final EditText edtnascimento = findViewById(R.id.edtNascimento);
        selecionado = null;
        edtnome.setText("");
        edtcpf.setText("");
        edtnascimento.setText("");
    }
}