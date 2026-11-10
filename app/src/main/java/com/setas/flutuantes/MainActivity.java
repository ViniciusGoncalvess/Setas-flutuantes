package com.setas.flutuantes;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private float d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("config", MODE_PRIVATE);
        d = getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);

        LinearLayout tela = new LinearLayout(this);
        tela.setOrientation(LinearLayout.VERTICAL);
        tela.setPadding(pad, pad, pad, pad);

        tela.addView(texto("Setas Flutuantes", 26));
        tela.addView(texto("Para usar: toque no botão abaixo, abra Aplicativos instalados, escolha Setas Flutuantes e ative a chave.", 15));

        Button abrir = new Button(this);
        abrir.setText("Abrir configurações de acessibilidade");
        abrir.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        tela.addView(abrir);

        tela.addView(texto("Estilo das teclas", 18));
        String[] nomes = {
                "Cruz (+) com alça no meio",
                "Teclado de PC (setas em T)",
                "Dividido: \u2191\u2193 à esquerda, \u2190\u2192 à direita",
                "Personalizado (arraste cada tecla)"};
        RadioGroup grupo = new RadioGroup(this);
        for (int i = 0; i < nomes.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(100 + i);
            rb.setText(nomes[i]);
            grupo.addView(rb);
        }
        grupo.check(100 + prefs.getInt("estilo", 1));
        grupo.setOnCheckedChangeListener((g, id) ->
                prefs.edit().putInt("estilo", id - 100).apply());
        tela.addView(grupo);

        CheckBox editar = new CheckBox(this);
        editar.setText("Editar posições (só no estilo Personalizado). Com isso ligado, as teclas não jogam: só arrastam. Desligue para jogar.");
        editar.setChecked(prefs.getBoolean("editar", false));
        editar.setOnCheckedChangeListener((b, marcado) ->
                prefs.edit().putBoolean("editar", marcado).apply());
        tela.addView(editar);

        tela.addView(texto("Ajustes das teclas (solte o dedo para aplicar)", 18));
        tela.addView(slider("Tamanho dos botões", 40, 120, "tam", 72));
        tela.addView(slider("Distância do \u2715 (não vale no Personalizado)", 0, 100, "folga", 16));

        tela.addView(texto("Cor das teclas", 16));
        LinearLayout cores = new LinearLayout(this);
        cores.setOrientation(LinearLayout.HORIZONTAL);
        int[] paleta = {0xFF455A64, 0xFF1E88E5, 0xFF43A047,
                0xFFE53935, 0xFF8E24AA, 0xFFFB8C00};
        int m = (int) (4 * d);
        for (final int c : paleta) {
            Button b = new Button(this);
            b.setBackgroundColor(c);
            b.setOnClickListener(v -> prefs.edit().putInt("cor", c).apply());
            LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(0, (int) (48 * d), 1f);
            p.setMargins(m, 0, m, 0);
            cores.addView(b, p);
        }
        tela.addView(cores);

        Button padrao = new Button(this);
        padrao.setText("Restaurar padrão");
        padrao.setOnClickListener(v -> {
            SharedPreferences.Editor ed = prefs.edit();
            for (String k : prefs.getAll().keySet()) {
                if (k.startsWith("pos_")) ed.remove(k);
            }
            ed.putInt("tam", 72).putInt("folga", 16)
                    .putInt("cor", 0xFF455A64).putInt("estilo", 1)
                    .putBoolean("editar", false)
                    .putLong("versao", System.currentTimeMillis());
            ed.apply();
            recreate();
        });
        tela.addView(padrao);

        ScrollView rolagem = new ScrollView(this);
        rolagem.addView(tela);
        setContentView(rolagem);
    }

    private TextView texto(String t, float sp) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(sp);
        v.setPadding(0, (int) (12 * d), 0, (int) (4 * d));
        return v;
    }

    private LinearLayout slider(final String rotulo, final int min, int max,
                                final String chave, int padrao) {
        LinearLayout caixa = new LinearLayout(this);
        caixa.setOrientation(LinearLayout.VERTICAL);

        final TextView label = texto("", 16);
        int atual = prefs.getInt(chave, padrao);
        label.setText(rotulo + ": " + atual);

        SeekBar barra = new SeekBar(this);
        barra.setMax(max - min);
        barra.setProgress(atual - min);
        barra.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progresso, boolean doUsuario) {
                label.setText(rotulo + ": " + (progresso + min));
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
                prefs.edit().putInt(chave, s.getProgress() + min).apply();
            }
        });

        caixa.addView(label);
        caixa.addView(barra);
        return caixa;
    }
}
