package com.setas.flutuantes;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;

import java.util.ArrayList;

public class SetasService extends AccessibilityService {

    // tipos: 0 cima, 1 esquerda, 2 baixo, 3 direita, 4 alça, 5 fechar, 6 alça 2
    private class Tecla {
        int tipo, grupo, bx, by;
        View view;
        WindowManager.LayoutParams lp;
    }

    private final ArrayList<Tecla> teclas = new ArrayList<>();
    private WindowManager wm;
    private SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener ouvinte;
    private int estilo, tam, passo, folga, cor;
    private float tamTexto;
    private boolean editando;
    private float toqueX, toqueY;
    private int baseX, baseY;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (!teclas.isEmpty()) return;

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        prefs = getSharedPreferences("config", MODE_PRIVATE);
        if (ouvinte == null) {
            ouvinte = (p, chave) -> {
                if (chave == null || !chave.startsWith("pos_")) {
                    reconstruir();
                }
            };
            prefs.registerOnSharedPreferenceChangeListener(ouvinte);
        }
        construir();
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    // devolve {coluna, linha, grupo} ou null se a tecla não existe nesse estilo
    private int[] celula(int est, int tipo) {
        switch (est) {
            case 0:
                switch (tipo) {
                    case 0: return new int[]{1, 0, 0};
                    case 1: return new int[]{0, 1, 0};
                    case 2: return new int[]{1, 2, 0};
                    case 3: return new int[]{2, 1, 0};
                    case 4: return new int[]{1, 1, 0};
                    case 5: return new int[]{2, 0, 0};
                }
                return null;
            case 2:
                switch (tipo) {
                    case 0: return new int[]{0, 0, 0};
                    case 2: return new int[]{0, 1, 0};
                    case 4: return new int[]{0, 2, 0};
                    case 5: return new int[]{1, 2, 0};
                    case 1: return new int[]{0, 0, 1};
                    case 3: return new int[]{1, 0, 1};
                    case 6: return new int[]{0, 1, 1};
                }
                return null;
            default:
                switch (tipo) {
                    case 0: return new int[]{1, 0, 0};
                    case 1: return new int[]{0, 1, 0};
                    case 2: return new int[]{1, 1, 0};
                    case 3: return new int[]{2, 1, 0};
                    case 4: return new int[]{0, 0, 0};
                    case 5: return new int[]{2, 0, 0};
                }
                return null;
        }
    }

    private int[] origemPadrao(int est, int grupo) {
        DisplayMetrics m = getResources().getDisplayMetrics();
        int maxC = 0, maxR = 0;
        for (int t = 0; t < 7; t++) {
            int[] c = celula(est, t);
            if (c != null && c[2] == grupo) {
                maxC = Math.max(maxC, c[0]);
                maxR = Math.max(maxR, c[1]);
            }
        }
        int w = (maxC + 1) * passo + folga;
        int h = (maxR + 1) * passo;
        int y = m.heightPixels - h - dp(90);
        int x = (est == 2 && grupo == 1) ? m.widthPixels - w - dp(16) : dp(16);
        return new int[]{x, y};
    }

    private int[] origem(int est, int grupo) {
        int[] pad = origemPadrao(est, grupo);
        String base = "pos_g" + est + "_" + grupo;
        return new int[]{prefs.getInt(base + "x", pad[0]), prefs.getInt(base + "y", pad[1])};
    }

    private void construir() {
        estilo = prefs.getInt("estilo", 1);
        int tamDp = prefs.getInt("tam", 72);
        tam = dp(tamDp);
        passo = tam + dp(4);
        folga = dp(prefs.getInt("folga", 16));
        cor = prefs.getInt("cor", 0xFF455A64);
        tamTexto = tamDp * 0.36f;
        editando = estilo == 3 && prefs.getBoolean("editar", false);

        for (int tipo = 0; tipo < 7; tipo++) {
            if (estilo == 3 && (tipo == 4 || tipo == 6)) continue;
            int[] c = celula(estilo, tipo);
            if (c == null) continue;
            int extra = (tipo == 5) ? folga : 0;
            int x, y;
            if (estilo == 3) {
                int[] o = origemPadrao(3, 0);
                x = prefs.getInt("pos_c" + tipo + "x", o[0] + c[0] * passo + extra);
                y = prefs.getInt("pos_c" + tipo + "y", o[1] + c[1] * passo);
            } else {
                int[] o = origem(estilo, c[2]);
                x = o[0] + c[0] * passo + extra;
                y = o[1] + c[1] * passo;
            }
            adicionarTecla(tipo, c[2], x, y);
        }
    }

    private void reconstruir() {
        if (teclas.isEmpty()) return;
        removerTudo();
        construir();
    }

    private void removerTudo() {
        for (Tecla t : teclas) {
            try {
                wm.removeView(t.view);
            } catch (Exception ex) {
            }
        }
        teclas.clear();
    }

    private void adicionarTecla(int tipo, int grupo, int x, int y) {
        String texto;
        switch (tipo) {
            case 0: texto = "\u2191"; break;
            case 1: texto = "\u2190"; break;
            case 2: texto = "\u2193"; break;
            case 3: texto = "\u2192"; break;
            case 5: texto = "\u2715"; break;
            default: texto = "\u2725"; break;
        }
        boolean pequena = tipo >= 4;

        Tecla t = new Tecla();
        t.tipo = tipo;
        t.grupo = grupo;
        t.view = botao(texto, pequena ? tamTexto * 0.85f : tamTexto);
        t.lp = new WindowManager.LayoutParams(
                tam, tam,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);
        t.lp.gravity = Gravity.TOP | Gravity.START;
        t.lp.x = x;
        t.lp.y = y;
        configurarToque(t);
        wm.addView(t.view, t.lp);
        teclas.add(t);
    }

    private Button botao(String texto, float sp) {
        Button b = new Button(this);
        b.setText(texto);
        b.setTextSize(sp);
        b.setTextColor(Color.WHITE);
        b.setPadding(0, 0, 0, 0);
        GradientDrawable fundo = new GradientDrawable();
        fundo.setColor(cor);
        fundo.setCornerRadius(dp(12));
        if (editando) {
            fundo.setStroke(dp(2), Color.WHITE);
        }
        b.setBackground(fundo);
        b.setAlpha(0.7f);
        return b;
    }

    private void configurarToque(final Tecla t) {
        t.view.setOnTouchListener((v, e) -> {
            int acao = e.getActionMasked();
            if (editando) {
                moverIndividual(t, e, acao);
                return true;
            }
            switch (t.tipo) {
                case 0:
                    if (acao == MotionEvent.ACTION_DOWN) deslizar(0, -1);
                    break;
                case 1:
                    if (acao == MotionEvent.ACTION_DOWN) deslizar(-1, 0);
                    break;
                case 2:
                    if (acao == MotionEvent.ACTION_DOWN) deslizar(0, 1);
                    break;
                case 3:
                    if (acao == MotionEvent.ACTION_DOWN) deslizar(1, 0);
                    break;
                case 5:
                    if (acao == MotionEvent.ACTION_UP) {
                        removerTudo();
                        disableSelf();
                    }
                    break;
                default:
                    moverGrupo(t, e, acao);
                    break;
            }
            return true;
        });
    }

    private void moverIndividual(Tecla t, MotionEvent e, int acao) {
        if (acao == MotionEvent.ACTION_DOWN) {
            toqueX = e.getRawX();
            toqueY = e.getRawY();
            baseX = t.lp.x;
            baseY = t.lp.y;
        } else if (acao == MotionEvent.ACTION_MOVE) {
            t.lp.x = baseX + (int) (e.getRawX() - toqueX);
            t.lp.y = baseY + (int) (e.getRawY() - toqueY);
            wm.updateViewLayout(t.view, t.lp);
        } else if (acao == MotionEvent.ACTION_UP || acao == MotionEvent.ACTION_CANCEL) {
            prefs.edit()
                    .putInt("pos_c" + t.tipo + "x", t.lp.x)
                    .putInt("pos_c" + t.tipo + "y", t.lp.y)
                    .apply();
        }
    }

    private void moverGrupo(Tecla alca, MotionEvent e, int acao) {
        if (acao == MotionEvent.ACTION_DOWN) {
            toqueX = e.getRawX();
            toqueY = e.getRawY();
            for (Tecla t : teclas) {
                if (t.grupo == alca.grupo) {
                    t.bx = t.lp.x;
                    t.by = t.lp.y;
                }
            }
        } else if (acao == MotionEvent.ACTION_MOVE) {
            int dx = (int) (e.getRawX() - toqueX);
            int dy = (int) (e.getRawY() - toqueY);
            for (Tecla t : teclas) {
                if (t.grupo == alca.grupo) {
                    t.lp.x = t.bx + dx;
                    t.lp.y = t.by + dy;
                    wm.updateViewLayout(t.view, t.lp);
                }
            }
        } else if (acao == MotionEvent.ACTION_UP || acao == MotionEvent.ACTION_CANCEL) {
            int[] c = celula(estilo, alca.tipo);
            if (c != null) {
                String base = "pos_g" + estilo + "_" + alca.grupo;
                prefs.edit()
                        .putInt(base + "x", alca.lp.x - c[0] * passo)
                        .putInt(base + "y", alca.lp.y - c[1] * passo)
                        .apply();
            }
        }
    }

    private void deslizar(float dx, float dy) {
        DisplayMetrics m = getResources().getDisplayMetrics();
        float cx = m.widthPixels / 2f;
        float cy = m.heightPixels / 2f;
        float dist = m.widthPixels * 0.3f;

        Path caminho = new Path();
        caminho.moveTo(cx - dx * dist / 2f, cy - dy * dist / 2f);
        caminho.lineTo(cx + dx * dist / 2f, cy + dy * dist / 2f);

        GestureDescription.StrokeDescription traco =
                new GestureDescription.StrokeDescription(caminho, 0, 60);
        dispatchGesture(new GestureDescription.Builder().addStroke(traco).build(), null, null);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(Intent intent) {
        removerTudo();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removerTudo();
        if (prefs != null && ouvinte != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(ouvinte);
        }
        ouvinte = null;
    }
}
