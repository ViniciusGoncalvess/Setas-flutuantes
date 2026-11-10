package com.setas.flutuantes;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.GridLayout;

public class SetasService extends AccessibilityService {

    private WindowManager wm;
    private GridLayout painel;
    private WindowManager.LayoutParams lp;
    private float toqueX, toqueY;
    private int baseX, baseY;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (painel != null) return;

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        float densidade = getResources().getDisplayMetrics().density;
        int tam = (int) (72 * densidade);

        painel = new GridLayout(this);
        painel.setRowCount(2);
        painel.setColumnCount(3);

        adicionar(criarAlca(), 0, 0, tam);
        adicionar(criarSeta("\u2191", 0, -1), 0, 1, tam);
        adicionar(criarFechar(), 0, 2, tam);
        adicionar(criarSeta("\u2190", -1, 0), 1, 0, tam);
        adicionar(criarSeta("\u2193", 0, 1), 1, 1, tam);
        adicionar(criarSeta("\u2192", 1, 0), 1, 2, tam);

        lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = (int) (16 * densidade);
        lp.y = getResources().getDisplayMetrics().heightPixels
                - (2 * tam) - (int) (100 * densidade);

        wm.addView(painel, lp);
    }

    private void adicionar(View v, int linha, int coluna, int tam) {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams(
                GridLayout.spec(linha), GridLayout.spec(coluna));
        p.width = tam;
        p.height = tam;
        painel.addView(v, p);
    }

    private Button criarSeta(String texto, final float dx, final float dy) {
        Button b = new Button(this);
        b.setText(texto);
        b.setTextSize(26);
        b.setPadding(0, 0, 0, 0);
        b.setAlpha(0.65f);
        b.setOnTouchListener((v, e) -> {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                deslizar(dx, dy);
            }
            return true;
        });
        return b;
    }

    private Button criarAlca() {
        Button b = new Button(this);
        b.setText("\u2725");
        b.setTextSize(22);
        b.setPadding(0, 0, 0, 0);
        b.setAlpha(0.65f);
        b.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    toqueX = e.getRawX();
                    toqueY = e.getRawY();
                    baseX = lp.x;
                    baseY = lp.y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    lp.x = baseX + (int) (e.getRawX() - toqueX);
                    lp.y = baseY + (int) (e.getRawY() - toqueY);
                    wm.updateViewLayout(painel, lp);
                    break;
            }
            return true;
        });
        return b;
    }

    private Button criarFechar() {
        Button b = new Button(this);
        b.setText("\u2715");
        b.setTextSize(22);
        b.setPadding(0, 0, 0, 0);
        b.setAlpha(0.65f);
        b.setOnTouchListener((v, e) -> {
            if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                removerPainel();
                disableSelf();
            }
            return true;
        });
        return b;
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

    private void removerPainel() {
        if (painel != null && wm != null) {
            try {
                wm.removeView(painel);
            } catch (Exception e) {
            }
            painel = null;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(Intent intent) {
        removerPainel();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removerPainel();
    }
}
