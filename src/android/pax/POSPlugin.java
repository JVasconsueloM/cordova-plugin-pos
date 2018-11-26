package pax;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

import com.pax.dal.IDAL;
import com.pax.dal.IPrinter;
import com.pax.dal.ISys;
import com.pax.dal.entity.EBeepMode;
import com.pax.dal.entity.EFontTypeAscii;
import com.pax.dal.entity.EFontTypeExtCode;
import com.pax.dal.exceptions.PrinterDevException;
import com.pax.gl.IGL;
import com.pax.gl.IGLProxy;
import com.pax.gl.imgprocessing.IImgProcessing.IPage.EAlign;
import com.pax.gl.imgprocessing.IImgProcessing.IPage.ILine.IUnit;
import com.pax.gl.imgprocessing.IImgProcessing.IPage.ILine;
import com.pax.gl.imgprocessing.IImgProcessing.IPage;
import com.pax.gl.imgprocessing.IImgProcessing;
import com.pax.gl.impl.*;
import com.pax.neptunelite.api.NeptuneLiteUser;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.lang.Exception;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult.Status;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class POSPlugin extends CordovaPlugin {
    private IPrinter printer;
    private static IDAL idal;
    private static IGL igl;
    private static ISys isys;
    private static final String PRINT = "PRINT";
    private static final String STATUS_PRINTER = "STATUS_PRINTER";
    private static final String TAG = "PRINTER";

    private static final String ALIGN_CENTER = "CENTER";
    private static final String ALIGN_LEFT = "LEFT";
    private static final String ALIGN_RIGHT = "RIGHT";
    private static final String FONT_EXTRA_LARGE = "EXTRA_LARGE";
    private static final String FONT_EXTRA_EXTRA_LARGE = "FONT_EXTRA_EXTRA_LARGE";
    private static final String FONT_LARGE = "LARGE";
    private static final String FONT_NORMAL = "NORMAL";
    private static final String FONT_SMALL = "SMALL";
    private static final String KEY_ALIGN = "TEXT_ALIGN";
    private static final String KEY_FONT_FACE = "FONT_FACE";
    private static final String KEY_LINE_SPACE = "LINE_SPACE";
    private static final String KEY_SIZE = "FONT_SIZE";
    private static final String KEY_STYLE = "TEXT_STYLE";
    private static final String KEY_TEXT = "TEXT";
    private static final String KEY_TOP_SPACE = "TOP_SPACE";
    private static final String KEY_WEIGHT = "TEXT_WEIGHT";
    private static final String STYLE_BOLD = "BOLD";
    private static final String STYLE_NORMAL = "NORMAL";
    private static final String STYLE_UNDERLINE = "UNDERLINE";
    private static final int MIN_WEIGHT = 1;
    private static final int NO_TOP_SPACE = 0;


    Map<String, EAlign> ALIGN = new HashMap<String, EAlign>(){{
      put(ALIGN_LEFT, EAlign.LEFT  );
      put(ALIGN_CENTER, EAlign.CENTER);
      put(ALIGN_RIGHT, EAlign.RIGHT );
    }};

    Map<String, Integer> TEXT_STYLE = new HashMap<String, Integer>(){{
      put(STYLE_NORMAL, IUnit.TEXT_STYLE_NORMAL);
      put(STYLE_BOLD, IUnit.TEXT_STYLE_BOLD);
      put(STYLE_UNDERLINE, IUnit.TEXT_STYLE_UNDERLINE);
    }};

    Map<String, Integer> FONT_SIZE = new HashMap<String, Integer>(){{
      put(FONT_SMALL, 16);
      put(FONT_NORMAL, 20);
      put(FONT_LARGE, 24);
      put(FONT_EXTRA_LARGE, 32);
      put(FONT_EXTRA_EXTRA_LARGE, 40);
    }};


    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    }


    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        try {
            boolean isPrintAction = PRINT.equals(action);
            boolean isStatusPrinterAction = STATUS_PRINTER.equals(action);  

           if ( !isPrintAction && !isStatusPrinterAction) {
                return false;
            }

            JSONArray content = args.optJSONArray(0);
            JSONObject options = args.optJSONObject(1);

            Context context = this.cordova.getActivity().getApplicationContext();

            Log.i(TAG,"GETTING IDAL/IGL/ISYS/PRINTER");
            getIDAL(context);
            getIGL(context);
            getISys();
            getPrinter();

            Log.i(TAG,"GETTING STATUS");
            String status = getStatus();
            Log.i(TAG, status);
            if(isStatusPrinterAction){ 
                returnSuccessMessage(callbackContext, status);
                return true;
            }
            if ( !status.contains("exito") ){ throw new Exception(status); }

            print(content, options);
            returnSuccessMessage(callbackContext, "finish");
            return true;

        } catch (Exception e) {
            Log.e(TAG,"Error General: ", e);
            returnErrorMessage(callbackContext, e.getMessage());
            return false;
        }
    }

    public void returnSuccessMessage(CallbackContext callbackContext, String message){
        beepOk();
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, message));
    }

    public void returnErrorMessage(CallbackContext callbackContext, String message){
        beepErr();
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, message));
    }

    public static void beepOk() {
        isys.beep(EBeepMode.FREQUENCE_LEVEL_2, 75);
        isys.beep(EBeepMode.FREQUENCE_LEVEL_3, 75);
        isys.beep(EBeepMode.FREQUENCE_LEVEL_4, 75);
        isys.beep(EBeepMode.FREQUENCE_LEVEL_5, 75);
    }

    public static void beepErr() {
        isys.beep(EBeepMode.FREQUENCE_LEVEL_6, 500);
    }

    public void getIDAL(Context context) throws Exception {
        idal = NeptuneLiteUser.getInstance().getDal(context);
    }

    public void getIGL(Context context) throws Exception {
        igl = GL.getInstance(context);
    }

    public void getISys() throws Exception {
        isys= idal.getSys();
    }

    public void getPrinter() throws Exception {
        printer = idal.getPrinter();
    }

    // Estado de impresión 
    public String getStatus() throws PrinterDevException {
        int status = printer.getStatus();
        return statusCode2Str(status);
    }

    public void print(JSONArray content, JSONObject options) throws Exception {
        Log.d(TAG, "printer print START");
        // try {
            // CONFIGURATION OPTIONS
            int lineSpace = options.optInt(KEY_LINE_SPACE, -2);
            // String fontFace = options.optString(KEY_FONT_FACE, "");

            IImgProcessing imgProcessing = igl.getImgProcessing();
            IPage page = imgProcessing.createPage();
            page.adjustLineSpace(lineSpace); 
            Log.d(TAG, "lineSpace: " + lineSpace);
            // page.setTypeFace(fontFace); // fuentes: exmple robotto.rff


            for (int i = 0; i < content.length(); i++) {
		      	JSONArray units = content.getJSONArray(i);
		      	ILine line = page.addLine();

	            for (int ii = 0; ii < units.length(); ii++) {
			      	JSONObject unit = units.getJSONObject(ii);
	                String text = unit.optString(KEY_TEXT);
	                String size = unit.optString(KEY_SIZE, FONT_NORMAL);
	                String align = unit.optString(KEY_ALIGN, ALIGN_LEFT);
	                String style = unit.optString(KEY_STYLE, STYLE_NORMAL);
	                Float weight = (float) unit.optInt(KEY_WEIGHT, MIN_WEIGHT);
	                int topSpace = unit.optInt(KEY_TOP_SPACE, NO_TOP_SPACE);

                    line.addUnit(
                        text, 
                        FONT_SIZE.get(size), 
                        ALIGN.get(align), 
                        TEXT_STYLE.get(style), 
                        weight
                    ).adjustTopSpace(topSpace);

            		// line.addUnit(getImageFromAssetsFile("pax_logo_normal.png"), EAlign.CENTER); tbn acepta imagenes

			    }

		    }
            init();
            printBitmap(imgProcessing.pageToBitmap(page, 384));
            start();
        // }
        /* catch (Exception e) {
            Log.e(TAG,"Error Printer",e);
            beepErr();
            this.callbackContext.error("" + e );
        }
        */

        Log.d(TAG, "printer print END");
    }

    // Inicializar
    public void init() throws PrinterDevException {
        printer.init();
    }


/*
    // Configuración de fuente (ASCII, código de extensión)
    public void fontSet(String asciiFontType, String cFontType) throws PrinterDevException {
        printer.fontSet(getEFontTypeAscii(asciiFontType), getEFontTypeExtCode(cFontType));
    }

    // ASCII
    public EFontTypeAscii getEFontTypeAscii(String asciiFontType){
        return EFONTTYPEASCII.get(asciiFontType);
    }

    // código de extensión
    public EFontTypeExtCode getEFontTypeExtCode(String cFontType){
        return EFONTTYPEEXTCODE.get(cFontType);
    }*/

    // Configuración de espaciado (espaciado entre palabras, interlineado)
    public void spaceSet(byte wordSpace, byte lineSpace) throws PrinterDevException {
        printer.spaceSet(wordSpace, lineSpace);
    }


    // Imprimir texto
    public void printStr(String str, String charset) throws PrinterDevException {
        printer.printStr(str, charset);
    }
    // Imprimir imagen
    public void printBitmap(Bitmap bitmap) throws PrinterDevException {
        printer.printBitmap(bitmap);
    }

    // Empezar a imprimir
    public String start() throws PrinterDevException {
        int res = printer.start();
        return statusCode2Str(res);

    }

    // Carácter imprime el borde izquierdo
    public void leftIndent(short indent) throws PrinterDevException {
        printer.leftIndent(indent);
    }

    public int getDotLine() throws PrinterDevException {
        int dotLine = printer.getDotLine();
        return dotLine;
    }

    // Opacidad (nivel de oscuridad) de la impresión
    public void setGray(int level) throws PrinterDevException {
        printer.setGray(level);

    }

    // Doble ancho <-> ancho normal
    public void setDoubleWidth(boolean isAscDouble, boolean isLocalDouble) throws PrinterDevException {
        printer.doubleWidth(isAscDouble, isLocalDouble);
    }

    // Doble altura <-> altura normal
    public void setDoubleHeight(boolean isAscDouble, boolean isLocalDouble) throws PrinterDevException {
        printer.doubleHeight(isAscDouble, isLocalDouble);

    }

    // Impresión normal <-> impresión inversa
    public void setInvert(boolean isInvert) throws PrinterDevException {
        printer.invert(isInvert);
    }

    // Papel
    public void step(int b) throws PrinterDevException {
        printer.step(b);
    }

    public String cutPaper(int mode) throws PrinterDevException {
        printer.cutPaper(mode);
        return "Corte de papel con éxito.";
    }

    public String getCutMode() throws PrinterDevException {
        String resultStr = "";
        int mode = printer.getCutMode();
        switch (mode) {
            case 0:
                resultStr = "Sólo admite corte de papel completo. ";
                break;
            case 1:
                resultStr = "Solo admite corte parcial de papel. ";
                break;
            case 2:
                resultStr = "Soporta papel parcial y corte de papel completo. ";
                break;
            case -1:
                resultStr = "Sin soporte: No tiene cuchilla de corte.";
                break;
            default:
                break;
        }
        return resultStr;
    }

    public String statusCode2Str(int status) {
        String res = "";
        switch (status) {
            case 0:
                res = "exito. ";
                break;
            case 1:
                res = "La impresora está ocupada. ";
                break;
            case 2:
                res = "Sin papel. ";
                break;
            case 3:
                res = "Error en el paquete de datos. ";
                break;
            case 4:
                res = "Mal funcionamiento de la impresora. ";
                break;
            case 8:
                res = "Impresora sobrecalentada. ";
                break;
            case 9:
                res = "El voltaje de la impresora es demasiado bajo. ";
                break;
            case 240:
                res = "La impresión no está terminada. ";
                break;
            case 252:
                res = "La impresora no ha instalado la librería de fuentes. ";
                break;
            case 254:
                res = "Paquete de datos es demasiado largo. ";
                break;
            default:
                break;
        }
        return res;
    }


}
