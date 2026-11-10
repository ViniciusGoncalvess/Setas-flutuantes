package com.setas.flutuantes;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);

        LinearLayout tela = new LinearLayout(this);
        tela.setOrientation(LinearLayout.VERTICAL);
        tela.setGravity(Gravity.CENTER);
        tela.setPadding(pad, pad, pad, pad);

        TextView texto = new TextView(this);
        texto.setText("Setas Flutuantes\n\nToque no botão abaixo, abra Aplicativos instalados, escolha Setas Flutuantes e ative a chave.");
        texto.setTextSize(18);
        texto.setGravity(Gravity.CENTER);

        Button botao = new Button(this);
        botao.setText("Abrir configurações de acessibilidade");
        botao.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        tela.addView(texto);
        tela.addView(botao);
        setContentView(tela);
    }
}
