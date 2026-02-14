package taz.womp.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import taz.womp.protection.auth.ClientLogin;
import taz.womp.utils.RenderUtils;
import taz.womp.utils.TextRenderer;
import taz.womp.utils.EncryptedString;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.*;

public class LoginScreen extends Screen {
    private EncryptedString username = EncryptedString.of("");
    private EncryptedString password = EncryptedString.of("");
    private boolean passwordActive = false;
    private boolean usernameActive = false;
    private boolean showPassword = false;
    private String error = "";
    private boolean loggingIn = false;


    private static final EncryptedString TITLE = EncryptedString.of("Womp Client Login");
    private static final EncryptedString USERNAME_LABEL = EncryptedString.of("Username");
    private static final EncryptedString PASSWORD_LABEL = EncryptedString.of("Password");
    private static final EncryptedString LOGIN_LABEL = EncryptedString.of("Login");
    private static final EncryptedString LOGGING_IN_LABEL = EncryptedString.of("Logging in...");
    private static final EncryptedString HIDE_LABEL = EncryptedString.of("Hide");
    private static final EncryptedString SHOW_LABEL = EncryptedString.of("Show");
    private static final EncryptedString INVALID_CREDENTIALS = EncryptedString.of("Invalid credentials");

    private static final int LOGIN_COOLDOWN_MS = 2000;
    private long loginCooldownMillis = 0;

    public LoginScreen() {
        super(Text.literal(LOGIN_LABEL.toString()));
    }

    @Override
    public void init() {
        super.init();
        this.usernameActive = true;
        this.passwordActive = false;
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {

        int scaledWidth = MinecraftClient.getInstance().getWindow().getWidth();
        int scaledHeight = MinecraftClient.getInstance().getWindow().getHeight();
        for (int i = 0; i < scaledHeight; i += 2) {
            float progress = (float) i / scaledHeight;
            int gradientAlpha = (int) (180 * (0.2f + 0.8f * progress));
            Color gradientColor = new Color(8, 12, 25, gradientAlpha);
            drawContext.fill(0, i, scaledWidth, i + 2, gradientColor.getRGB());
        }


        RenderUtils.unscaledProjection();
        int fbWidth = MinecraftClient.getInstance().getWindow().getFramebufferWidth();
        int fbHeight = MinecraftClient.getInstance().getWindow().getFramebufferHeight();
        double scaleFactor = MinecraftClient.getInstance().getWindow().getScaleFactor();
        int sMouseX = (int) (mouseX * scaleFactor);
        int sMouseY = (int) (mouseY * scaleFactor);


        super.render(drawContext, sMouseX, sMouseY, delta);


        int width = fbWidth;
        int height = fbHeight;

        int boxWidth = Math.min(320, width - 100); // Responsive width
        int boxHeight = Math.min(220, height - 100); // Responsive height
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        


        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(100, 150, 255, 30),
                boxX - 2, boxY - 2, boxX + boxWidth + 2, boxY + boxHeight + 2, 16, 16, 16, 16, 50);
        

        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(20, 25, 35, 180),
                boxX, boxY, boxX + boxWidth, boxY + boxHeight, 14, 14, 14, 14, 50);


        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(40, 50, 70, 100),
                boxX + 1, boxY + 1, boxX + boxWidth - 1, boxY + boxHeight - 1, 13, 13, 13, 13, 50);


        long time = System.currentTimeMillis();
        float glowIntensity = (float) (0.5f + 0.3f * Math.sin(time * 0.002f));
        Color glowColor = new Color(100, 180, 255, (int)(glowIntensity * 120));
        RenderUtils.renderRoundedOutline(drawContext, glowColor,
                boxX, boxY, boxX + boxWidth, boxY + boxHeight, 14, 14, 14, 14, 1.5f, 50);


        TextRenderer.drawCenteredString(TITLE.toString(), drawContext, width / 2, boxY + 30, 
                new Color(200, 220, 255).getRGB());


        int fieldWidth = Math.min(260, boxWidth - 40); // Responsive field width
        int fieldHeight = 36;
        int fieldX = width / 2 - fieldWidth / 2;
        int userY = boxY + 60;
        int passY = boxY + 105;


        Color activeFieldBg = new Color(30, 35, 45, 200);
        Color inactiveFieldBg = new Color(25, 30, 40, 180);
        Color activeFieldBorder = new Color(120, 200, 255, 255);
        Color inactiveFieldBorder = new Color(60, 80, 100, 150);



        if (usernameActive) {
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(120, 200, 255, 40),
                    fieldX - 1, userY - 1, fieldX + fieldWidth + 1, userY + fieldHeight + 1, 8, 8, 8, 8, 30);
        }
        

        RenderUtils.renderRoundedQuad(drawContext.getMatrices(),
                usernameActive ? activeFieldBg : inactiveFieldBg,
                fieldX, userY, fieldX + fieldWidth, userY + fieldHeight, 8, 8, 8, 8, 30);


        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(50, 60, 80, 80),
                fieldX + 1, userY + 1, fieldX + fieldWidth - 1, userY + fieldHeight - 1, 7, 7, 7, 7, 30);


        RenderUtils.renderRoundedOutline(drawContext, 
                usernameActive ? activeFieldBorder : inactiveFieldBorder,
                fieldX, userY, fieldX + fieldWidth, userY + fieldHeight, 8, 8, 8, 8, 1.5f, 30);


        TextRenderer.drawString(USERNAME_LABEL.toString(), drawContext, fieldX + 12, userY + 8,
                new Color(160, 170, 180).getRGB());
        String usernameStr = username.toString();

        int textX = fieldX + 12 + TextRenderer.getWidth(USERNAME_LABEL.toString()) + 8;
        TextRenderer.drawString(usernameStr.isEmpty() && usernameActive ? "|" : usernameStr, drawContext, textX,
                userY + 8, new Color(220, 230, 240).getRGB());



        if (passwordActive) {
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(120, 200, 255, 40),
                    fieldX - 1, passY - 1, fieldX + fieldWidth + 1, passY + fieldHeight + 1, 8, 8, 8, 8, 30);
        }
        

        RenderUtils.renderRoundedQuad(drawContext.getMatrices(),
                passwordActive ? activeFieldBg : inactiveFieldBg,
                fieldX, passY, fieldX + fieldWidth, passY + fieldHeight, 8, 8, 8, 8, 30);


        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(50, 60, 80, 80),
                fieldX + 1, passY + 1, fieldX + fieldWidth - 1, passY + fieldHeight - 1, 7, 7, 7, 7, 30);


        RenderUtils.renderRoundedOutline(drawContext, 
                passwordActive ? activeFieldBorder : inactiveFieldBorder,
                fieldX, passY, fieldX + fieldWidth, passY + fieldHeight, 8, 8, 8, 8, 1.5f, 30);

        TextRenderer.drawString(PASSWORD_LABEL.toString(), drawContext, fieldX + 12, passY + 8,
                new Color(160, 170, 180).getRGB());
        String passwordStr = password.toString();
        String passDisplay = showPassword ? passwordStr : passwordStr.replaceAll(".", "*");


        int showBtnWidth = 45;
        int showBtnHeight = 22;
        int showBtnX = fieldX + fieldWidth - showBtnWidth - 10;
        int showBtnY = passY + (fieldHeight - showBtnHeight) / 2;


        int labelWidth = TextRenderer.getWidth(PASSWORD_LABEL.toString());
        int textStartX = fieldX + 12 + labelWidth + 8;
        int maxPassTextWidth = showBtnX - textStartX - 8;
        String clippedPassDisplay = passDisplay;
        while (TextRenderer.getWidth(clippedPassDisplay) > maxPassTextWidth && clippedPassDisplay.length() > 0) {
            clippedPassDisplay = clippedPassDisplay.substring(1);
        }
        TextRenderer.drawString(clippedPassDisplay.isEmpty() && passwordActive ? "|" : clippedPassDisplay, drawContext,
                textStartX, passY + 8, new Color(220, 230, 240).getRGB());


        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(40, 50, 65, 220), showBtnX, showBtnY,
                showBtnX + showBtnWidth, showBtnY + showBtnHeight, 6, 6, 6, 6, 20);
        RenderUtils.renderRoundedOutline(drawContext, new Color(80, 120, 160, 150), showBtnX, showBtnY,
                showBtnX + showBtnWidth, showBtnY + showBtnHeight, 6, 6, 6, 6, 1, 20);
        TextRenderer.drawCenteredString((showPassword ? HIDE_LABEL : SHOW_LABEL).toString(), drawContext,
                showBtnX + showBtnWidth / 2, showBtnY + 4, new Color(180, 190, 200).getRGB());


        int btnY = boxY + 160;
        

        float buttonGlow = (float) (0.7f + 0.3f * Math.sin(time * 0.003f));
        Color buttonColor = new Color(120, 200, 255, (int)(220 * buttonGlow));
        Color buttonHoverColor = new Color(140, 220, 255, 255);

        boolean isHoveringButton = sMouseX >= fieldX && sMouseX <= fieldX + fieldWidth &&
                sMouseY >= btnY && sMouseY <= btnY + fieldHeight;


        if (isHoveringButton) {
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(140, 220, 255, 60),
                    fieldX - 2, btnY - 2, fieldX + fieldWidth + 2, btnY + fieldHeight + 2, 10, 10, 10, 10, 40);
        }


        RenderUtils.renderRoundedQuad(drawContext.getMatrices(),
                isHoveringButton ? buttonHoverColor : buttonColor,
                fieldX, btnY, fieldX + fieldWidth, btnY + fieldHeight, 10, 10, 10, 10, 40);


        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(160, 240, 255, 100),
                fieldX + 1, btnY + 1, fieldX + fieldWidth - 1, btnY + fieldHeight - 1, 9, 9, 9, 9, 40);


        RenderUtils.renderRoundedOutline(drawContext, new Color(180, 240, 255, 200),
                fieldX, btnY, fieldX + fieldWidth, btnY + fieldHeight, 10, 10, 10, 10, 1.5f, 40);

        TextRenderer.drawCenteredString(loggingIn ? LOGGING_IN_LABEL.toString() : LOGIN_LABEL.toString(),
                drawContext, width / 2, btnY + fieldHeight / 2 - 6, new Color(240, 250, 255).getRGB());


        if (!error.isEmpty()) {
            TextRenderer.drawCenteredString(error, drawContext, width / 2, btnY + fieldHeight + 20,
                    new Color(255, 120, 120).getRGB());
        }


        RenderUtils.scaledProjection();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (System.currentTimeMillis() < loginCooldownMillis)
            return true;
        if (MinecraftClient.getInstance().currentScreen != this)
            return true;


        double scaleFactor = MinecraftClient.getInstance().getWindow().getScaleFactor();
        double mX = mouseX * scaleFactor;
        double mY = mouseY * scaleFactor;
        int width = MinecraftClient.getInstance().getWindow().getFramebufferWidth();
        int height = MinecraftClient.getInstance().getWindow().getFramebufferHeight();


        int boxWidth = Math.min(320, width - 100);
        int boxHeight = Math.min(220, height - 100);
        int boxY = height / 2 - boxHeight / 2;

        int fieldWidth = Math.min(260, boxWidth - 40);
        int fieldHeight = 36;
        int fieldX = width / 2 - fieldWidth / 2;
        int userY = boxY + 60;
        int passY = boxY + 105;
        int btnY = boxY + 160;
        int showBtnX = fieldX + fieldWidth - 45 - 10;
        int showBtnY = passY + (fieldHeight - 22) / 2;
        int showBtnWidth = 45;
        int showBtnHeight = 22;


        if (mX >= fieldX && mX <= fieldX + fieldWidth && mY >= userY && mY <= userY + fieldHeight) {
            usernameActive = true;
            passwordActive = false;
            return true;
        }


        if (mX >= fieldX && mX <= fieldX + fieldWidth && mY >= passY && mY <= passY + fieldHeight) {
            if (!(mX >= showBtnX && mX <= showBtnX + showBtnWidth && mY >= showBtnY
                    && mY <= showBtnY + showBtnHeight)) {
                passwordActive = true;
                usernameActive = false;
                return true;
            }
        }


        if (mX >= showBtnX && mX <= showBtnX + showBtnWidth && mY >= showBtnY
                && mY <= showBtnY + showBtnHeight) {
            showPassword = !showPassword;
            return true;
        }


        if (mX >= fieldX && mX <= fieldX + fieldWidth && mY >= btnY && mY <= btnY + fieldHeight) {
            tryLogin();
            return true;
        }


        usernameActive = false;
        passwordActive = false;

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (System.currentTimeMillis() < loginCooldownMillis)
            return true;
        if (MinecraftClient.getInstance().currentScreen != this)
            return true;

        if (usernameActive) {
            String usernameStr = username.toString();
            if ((chr == '\b' || chr == 127) && !usernameStr.isEmpty()) {
                username.close();
                username = EncryptedString.of(usernameStr.substring(0, usernameStr.length() - 1));
            } else if (chr >= 32 && chr < 127 && usernameStr.length() < 32) {
                username.close();
                username = EncryptedString.of(usernameStr + chr);
            }
            return true;
        }

        if (passwordActive) {
            String passwordStr = password.toString();
            if ((chr == '\b' || chr == 127) && !passwordStr.isEmpty()) {
                password.close();
                password = EncryptedString.of(passwordStr.substring(0, passwordStr.length() - 1));
            } else if (chr >= 32 && chr < 127 && passwordStr.length() < 32) {
                password.close();
                password = EncryptedString.of(passwordStr + chr);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (System.currentTimeMillis() < loginCooldownMillis)
            return true;
        if (MinecraftClient.getInstance().currentScreen != this)
            return true;


        if (keyCode == 259) {
            if (usernameActive) {
                String usernameStr = username.toString();
                if (!usernameStr.isEmpty()) {
                    username.close();
                    username = EncryptedString.of(usernameStr.substring(0, usernameStr.length() - 1));
                }
                return true;
            }
            if (passwordActive) {
                String passwordStr = password.toString();
                if (!passwordStr.isEmpty()) {
                    password.close();
                    password = EncryptedString.of(passwordStr.substring(0, passwordStr.length() - 1));
                }
                return true;
            }
        }
        if (keyCode == 261) {
            if (usernameActive) {
                String usernameStr = username.toString();
                if (!usernameStr.isEmpty()) {
                    username.close();
                    username = EncryptedString.of(usernameStr.substring(0, usernameStr.length() - 1));
                }
                return true;
            }
            if (passwordActive) {
                String passwordStr = password.toString();
                if (!passwordStr.isEmpty()) {
                    password.close();
                    password = EncryptedString.of(passwordStr.substring(0, passwordStr.length() - 1));
                }
                return true;
            }
        }

        if (keyCode == 258) { // Tab
            if (usernameActive) {
                usernameActive = false;
                passwordActive = true;
            } else {
                usernameActive = true;
                passwordActive = false;
            }
            return true;
        }

        if (keyCode == 257) { // Enter
            tryLogin();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void tryLogin() {
        if (loggingIn)
            return;
        if (System.currentTimeMillis() < loginCooldownMillis)
            return;

        loggingIn = true;
        error = "";
        String usernameStr = username.toString();
        String passwordStr = password.toString();

        try {
            String response = ClientLogin.run(usernameStr, passwordStr);

            if (response != null && response.contains("Successfully processed login!")) {
                String postLoginHWID = taz.womp.protection.HWIDGrabber.getHWID();
                if (postLoginHWID == null || postLoginHWID.trim().isEmpty()) {
                    taz.womp.manager.ProtectionManager.exit("");
                    return;
                }


                if (!taz.womp.protection.auth.ClientLogin.checkHWID()) {
                    taz.womp.manager.ProtectionManager.exit("");
                    return;
                }


                try {
                    JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                    int uid = json.get("uid").getAsInt();
                    String username = json.get("username").getAsString();
                    String email = json.get("email").getAsString();
                    String id = json.get("id").getAsString();
                    String jwtToken = json.get("jwtToken").getAsString();
                    taz.womp.Womp.INSTANCE
                            .setSession(new taz.womp.protection.Session(uid, username, email, id, "", jwtToken));
                } catch (Exception e) {
                    e.printStackTrace();
                    taz.womp.manager.ProtectionManager.exit("Session parse error");
                    return;
                }

                net.minecraft.client.MinecraftClient.getInstance().setScreen(null);
                loggingIn = false;
                return;
            }

            error = INVALID_CREDENTIALS.toString();
        } catch (Exception ex) {
            error = "Internal error whilst logging in, please contact the developer.";
            ex.printStackTrace();
        }

        password.close();
        password = taz.womp.utils.EncryptedString.of("");
        loggingIn = false;
        loginCooldownMillis = System.currentTimeMillis() + LOGIN_COOLDOWN_MS;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    public static boolean isLoginScreenOpen() {
        return MinecraftClient.getInstance().currentScreen instanceof LoginScreen;
    }
}