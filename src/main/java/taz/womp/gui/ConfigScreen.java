package taz.womp.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import taz.womp.utils.RenderUtils;
import taz.womp.utils.TextRenderer;
import taz.womp.protection.auth.GetConfig;
import taz.womp.protection.auth.DeleteConfig;
import taz.womp.protection.auth.GetConfigData;
import taz.womp.protection.auth.CreateConfig;
import taz.womp.protection.auth.UpdateConfig;
import taz.womp.manager.ConfigManager;
import taz.womp.protection.auth.ShareConfig;
import com.google.gson.*;
import taz.womp.utils.EncryptedString;
import taz.womp.protection.auth.UnshareConfig;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    private enum Tab { PERSONAL, PUBLIC, SHARED }
    private Tab currentTab = Tab.PERSONAL;

    private static class ConfigEntry {
        EncryptedString name;
        EncryptedString description;
        EncryptedString creator;
        EncryptedString sharedBy;
        boolean isPublic;
        boolean isApproved;
        boolean isOwn;

    }

    private final List<ConfigEntry> personalConfigs = new ArrayList<>();
    private final List<ConfigEntry> publicConfigs = new ArrayList<>();
    private final List<ConfigEntry> sharedConfigs = new ArrayList<>();

    private int selectedShareIndex = -1;
    private EncryptedString sharePromptInput = null;
    private boolean sharePromptActive = false;
    private EncryptedString feedbackMessage = null;
    private long feedbackTime = 0;

    private boolean createPromptActive = false;
    private EncryptedString createPromptName = EncryptedString.of("");
    private EncryptedString createPromptDesc = EncryptedString.of("");
    private boolean nameFieldActive = true;
    private boolean descFieldActive = false;


    private int shareUserScroll = 0;
    private static final int MAX_VISIBLE_SHARED_USERS = 5;
    private List<EncryptedString> shareDialogUsers = new ArrayList<>();
    private long lastShareDialogFetch = 0;

    private enum Label {
        CREATE_CONFIG,
        NO_CONFIGS_FOUND,
        CREATE_YOUR_FIRST_CONFIG,
        LOAD,
        SAVE,
        DELETE,
        SHARE,
        SHARE_CONFIG,
        ENTER_USERNAME,
        PENDING,
        NAME,
        DESCRIPTION,
        CREATE,
        X,
        ADDED_BY,
        SHARED_BY
    }

    private static String formatLabel(Label label) {
        String raw = label.name().toLowerCase().replace('_', ' ');
        String[] parts = raw.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
            if (i < parts.length - 1) sb.append(' ');
        }
        return sb.toString();
    }

    private static EncryptedString label(Label label) {
        return EncryptedString.of(formatLabel(label));
    }

    public ConfigScreen() {
        super(Text.literal("Configs"));
        refreshConfigs();
    }

    private void refreshConfigs() {
        personalConfigs.clear();
        publicConfigs.clear();
        sharedConfigs.clear();
        String response = GetConfig.run();
        if (response == null || response.isEmpty() || response.startsWith("Internal error")) return;
        try {
            JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
            JsonArray personal = obj.has("personal") ? obj.getAsJsonArray("personal") : new JsonArray();
            JsonArray pub = obj.has("public") ? obj.getAsJsonArray("public") : new JsonArray();
            JsonArray shared = obj.has("shared") ? obj.getAsJsonArray("shared") : new JsonArray();
            for (JsonElement el : personal) {
                JsonObject o = el.getAsJsonObject();
                ConfigEntry e = new ConfigEntry();
                e.name = EncryptedString.of(o.get("config_name").getAsString());
                e.creator = o.has("creator") ? EncryptedString.of(o.get("creator").getAsString()) : EncryptedString.of("");
                e.isPublic = o.has("public") && o.get("public").getAsInt() == 1;
                e.isApproved = o.has("approved") && o.get("approved").getAsInt() == 1;
                e.isOwn = true;
                personalConfigs.add(e);
            }
            for (JsonElement el : pub) {
                JsonObject o = el.getAsJsonObject();
                ConfigEntry e = new ConfigEntry();
                e.name = EncryptedString.of(o.get("config_name").getAsString());
                e.creator = o.has("creator") ? EncryptedString.of(o.get("creator").getAsString()) : EncryptedString.of("");
                e.isPublic = true;
                e.isApproved = true;
                e.isOwn = false;
                publicConfigs.add(e);
            }
            for (JsonElement el : shared) {
                JsonObject o = el.getAsJsonObject();
                ConfigEntry e = new ConfigEntry();
                e.name = EncryptedString.of(o.get("config_name").getAsString());
                e.creator = o.has("creator") ? EncryptedString.of(o.get("creator").getAsString()) : EncryptedString.of("");
                e.sharedBy = o.has("shared_by") ? EncryptedString.of(o.get("shared_by").getAsString()) : EncryptedString.of("");
                e.isPublic = false;
                e.isApproved = true;
                e.isOwn = false;
                sharedConfigs.add(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showFeedback(String msg) {
        feedbackMessage = EncryptedString.of(msg);
        feedbackTime = System.currentTimeMillis();
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        RenderUtils.unscaledProjection();
        int fbWidth = MinecraftClient.getInstance().getWindow().getFramebufferWidth();
        int fbHeight = MinecraftClient.getInstance().getWindow().getFramebufferHeight();
        double scaleFactor = MinecraftClient.getInstance().getWindow().getScaleFactor();
        int sMouseX = (int) (mouseX * scaleFactor);
        int sMouseY = (int) (mouseY * scaleFactor);


        super.render(drawContext, sMouseX, sMouseY, delta);

        drawContext.fill(0, 0, fbWidth, fbHeight, new Color(0, 0, 0, 200).getRGB());

        int boxWidth = 600;
        int boxHeight = 400;
        int boxX = fbWidth / 2 - boxWidth / 2;
        int boxY = fbHeight / 2 - boxHeight / 2;

        int tabWidth = 120;
        int tabHeight = 36;
        int tabY = boxY + 16;
        int tabSpacing = 8;
        int tabStartX = boxX + 32;

        int listX = boxX + 32;
        int listY = boxY + 70;
        int entryHeight = 48;
        int entrySpacing = 12;
        List<ConfigEntry> configsToShow = switch (currentTab) {
            case PERSONAL -> personalConfigs;
            case PUBLIC -> publicConfigs;
            case SHARED -> sharedConfigs;
        };
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(24, 24, 28, 240), boxX, boxY, boxX + boxWidth, boxY + boxHeight, 15, 15, 15, 15, 30);
        RenderUtils.renderRoundedOutline(drawContext, new Color(60, 120, 200, 220), boxX, boxY, boxX + boxWidth, boxY + boxHeight, 15, 15, 15, 15, 2, 30);


        for (Tab tab : Tab.values()) {
            int tabX = tabStartX + tab.ordinal() * (tabWidth + tabSpacing);
            boolean selected = (tab == currentTab);
            Color tabColor = selected ? new Color(60, 120, 200, 220) : new Color(40, 40, 50, 255);
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), tabColor, tabX, tabY, tabX + tabWidth, tabY + tabHeight, 8, 8, 8, 8, 20);
            TextRenderer.drawCenteredString(EncryptedString.of(tab.name().substring(0, 1) + tab.name().substring(1).toLowerCase()), drawContext, tabX + tabWidth / 2, tabY + 10, Color.WHITE.getRGB());
        }


        if (currentTab == Tab.PERSONAL) {
            int btnW = 140, btnH = 36;
            int btnX = boxX + boxWidth - btnW - 32;
            int btnY = boxY + 16;
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(60, 200, 120, 220), btnX, btnY, btnX + btnW, btnY + btnH, 8, 8, 8, 8, 20);
            TextRenderer.drawCenteredString(label(Label.CREATE_CONFIG), drawContext, btnX + btnW / 2, btnY + 10, Color.WHITE.getRGB());
        }

        if (configsToShow.isEmpty()) {
            TextRenderer.drawCenteredString(label(Label.NO_CONFIGS_FOUND), drawContext, boxX + boxWidth / 2, listY + 40, new Color(180, 180, 180).getRGB());
            if (currentTab == Tab.PERSONAL) {
                int btnW = 200, btnH = 48;
                int btnX = boxX + boxWidth / 2 - btnW / 2;
                int btnY = listY + 80;
                RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(60, 200, 120, 220), btnX, btnY, btnX + btnW, btnY + btnH, 10, 10, 10, 10, 30);
                TextRenderer.drawCenteredString(label(Label.CREATE_YOUR_FIRST_CONFIG), drawContext, btnX + btnW / 2, btnY + 16, Color.WHITE.getRGB());
            }
        }


        for (int i = 0; i < configsToShow.size(); i++) {
            ConfigEntry entry = configsToShow.get(i);
            int entryY = listY + i * (entryHeight + entrySpacing);

            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(30, 30, 35, 255), listX, entryY, listX + boxWidth - 64, entryY + entryHeight, 8, 8, 8, 8, 20);

            TextRenderer.drawString(entry.name == null ? EncryptedString.of("") : entry.name, drawContext, listX + 16, entryY + 10, Color.WHITE.getRGB());

            if (entry.description != null && entry.description.length() > 0) {
                TextRenderer.drawString(entry.description == null ? EncryptedString.of("") : entry.description, drawContext, listX + 16, entryY + 28, new Color(180, 180, 180).getRGB());
            }

            if (currentTab == Tab.PUBLIC) {
                TextRenderer.drawString(EncryptedString.of(formatLabel(Label.ADDED_BY) + ": " + (entry.creator == null ? "" : entry.creator.toString())), drawContext, listX + 220, entryY + 10, new Color(120, 210, 255).getRGB());
            } else if (currentTab == Tab.SHARED) {
                TextRenderer.drawString(EncryptedString.of(formatLabel(Label.SHARED_BY) + ": " + (entry.sharedBy == null ? "" : entry.sharedBy.toString())), drawContext, listX + 16, entryY + 28, new Color(255, 180, 120).getRGB());
                if (entry.creator != null && entry.creator.length() > 0) {
                    TextRenderer.drawString(EncryptedString.of(formatLabel(Label.ADDED_BY) + ": " + entry.creator.toString()), drawContext, listX + 220, entryY + 28, new Color(120, 210, 255).getRGB());
                }
            }

            if (entry.isOwn && entry.isPublic && !entry.isApproved) {
                TextRenderer.drawString(label(Label.PENDING), drawContext, listX + boxWidth - 140, entryY + 10, new Color(255, 220, 60).getRGB());
            }

            int btnW = 60;
            int btnH = 28;
            int btnY = entryY + (entryHeight - btnH) / 2;
            int spacing = 5;
            int entryRightX = listX + boxWidth - 64;
            int buttonRightMargin = 16;

            boolean isPersonal = currentTab == Tab.PERSONAL;

            int shareBtnX = 0, deleteBtnX = 0, saveBtnX = 0, loadBtnX = 0;

            if (isPersonal) {


                shareBtnX = entryRightX - buttonRightMargin - btnW;
                deleteBtnX = shareBtnX - spacing - btnW;
                saveBtnX = deleteBtnX - spacing - btnW;
                loadBtnX = saveBtnX - spacing - btnW;
            } else {
                shareBtnX = entryRightX - buttonRightMargin - btnW;
                deleteBtnX = shareBtnX - spacing - btnW;
                loadBtnX = deleteBtnX - spacing - btnW;
            }


            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(60, 120, 200, 220), loadBtnX, btnY, loadBtnX + btnW, btnY + btnH, 6, 6, 6, 6, 10);
            TextRenderer.drawCenteredString(label(Label.LOAD), drawContext, loadBtnX + btnW / 2, btnY + 7, Color.WHITE.getRGB());


            if (isPersonal) {
                RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(60, 200, 120, 220), saveBtnX, btnY, saveBtnX + btnW, btnY + btnH, 6, 6, 6, 6, 10);
                TextRenderer.drawCenteredString(label(Label.SAVE), drawContext, saveBtnX + btnW / 2, btnY + 7, Color.WHITE.getRGB());
            }


            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(200, 60, 60, 220), deleteBtnX, btnY, deleteBtnX + btnW, btnY + btnH, 6, 6, 6, 6, 10);
            TextRenderer.drawCenteredString(label(Label.DELETE), drawContext, deleteBtnX + btnW / 2, btnY + 7, Color.WHITE.getRGB());


            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(120, 210, 255, 220), shareBtnX, btnY, shareBtnX + btnW, btnY + btnH, 6, 6, 6, 6, 10);
            TextRenderer.drawCenteredString(label(Label.SHARE), drawContext, shareBtnX + btnW / 2, btnY + 7, Color.WHITE.getRGB());


        }


        if (sharePromptActive) {
            int dialogW = 320;
            int dialogH = 120 + 32 + 28 * Math.min(MAX_VISIBLE_SHARED_USERS, shareDialogUsers.size());
            int dialogX = fbWidth / 2 - dialogW / 2;
            int dialogY = fbHeight / 2 - dialogH / 2;
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(40, 40, 50, 240), dialogX, dialogY, dialogX + dialogW, dialogY + dialogH, 10, 10, 10, 10, 30);
            RenderUtils.renderRoundedOutline(drawContext, new Color(120, 210, 255, 220), dialogX, dialogY, dialogX + dialogW, dialogY + dialogH, 10, 10, 10, 10, 2, 30);
            TextRenderer.drawCenteredString(label(Label.SHARE_CONFIG), drawContext, fbWidth / 2, dialogY + 24, Color.WHITE.getRGB());
            TextRenderer.drawString(label(Label.ENTER_USERNAME), drawContext, dialogX + 32, dialogY + 56, Color.WHITE.getRGB());
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(30, 30, 35, 255), dialogX + 32, dialogY + 76, dialogX + dialogW - 32, dialogY + 100, 6, 6, 6, 6, 10);
            TextRenderer.drawString(sharePromptInput == null || sharePromptInput.length() == 0 ? EncryptedString.of("|") : sharePromptInput, drawContext, dialogX + 40, dialogY + 82, Color.WHITE.getRGB());

            int shareBtnW = 80, shareBtnH = 28;
            int shareBtnX = dialogX + dialogW / 2 - shareBtnW / 2;
            int shareBtnY = dialogY + 104;
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(120, 210, 255, 220), shareBtnX, shareBtnY, shareBtnX + shareBtnW, shareBtnY + shareBtnH, 8, 8, 8, 8, 20);
            TextRenderer.drawCenteredString(label(Label.SHARE), drawContext, shareBtnX + shareBtnW / 2, shareBtnY + 7, Color.WHITE.getRGB());

            if (selectedShareIndex >= 0 && (System.currentTimeMillis() - lastShareDialogFetch > 1000 || shareDialogUsers.isEmpty())) {
                String configName = personalConfigs.get(selectedShareIndex).name.toString();
                List<String> users = ShareConfig.getSharedUsers(configName);
                shareDialogUsers = new ArrayList<>();
                for (String user : users) shareDialogUsers.add(EncryptedString.of(user));
                lastShareDialogFetch = System.currentTimeMillis();
            }

            int userListY = shareBtnY + shareBtnH + 8;
            int userH = 28;
            int visible = Math.min(MAX_VISIBLE_SHARED_USERS, shareDialogUsers.size());
            int scrollMax = Math.max(0, shareDialogUsers.size() - MAX_VISIBLE_SHARED_USERS);
            for (int i = 0; i < visible; i++) {
                int idx = i + shareUserScroll;
                if (idx >= shareDialogUsers.size()) break;
                EncryptedString user = shareDialogUsers.get(idx);
                int userY = userListY + i * userH;
                RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(30, 30, 35, 255), dialogX + 32, userY, dialogX + dialogW - 32, userY + userH - 4, 6, 6, 6, 6, 10);
                TextRenderer.drawString(user == null ? EncryptedString.of("") : user, drawContext, dialogX + 40, userY + 7, Color.WHITE.getRGB());

                int xBtnSize = 22;
                int xBtnX = dialogX + dialogW - 32 - xBtnSize - 6;
                int xBtnY = userY + (userH - xBtnSize) / 2;
                RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(200, 60, 60, 220), xBtnX, xBtnY, xBtnX + xBtnSize, xBtnY + xBtnSize, xBtnSize / 2, xBtnSize / 2, xBtnSize / 2, xBtnSize / 2, xBtnSize);
                TextRenderer.drawCenteredString(label(Label.X), drawContext, xBtnX + xBtnSize / 2 - 2, xBtnY + xBtnSize / 2 - 4, Color.WHITE.getRGB());
            }

            if (scrollMax > 0) {
                int barH = visible * userH;
                int barY = userListY;
                int barX = dialogX + dialogW - 20;
                int scrollBarH = (int) (barH * (visible / (float) shareDialogUsers.size()));
                int scrollBarY = barY + (int) (barH * (shareUserScroll / (float) shareDialogUsers.size()));
                RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(120, 210, 255, 180), barX, scrollBarY, barX + 8, scrollBarY + scrollBarH, 4, 4, 4, 4, 8);
            }
        }


        if (feedbackMessage != null && System.currentTimeMillis() - feedbackTime < 3000) {
            TextRenderer.drawCenteredString(feedbackMessage == null ? EncryptedString.of("") : feedbackMessage, drawContext, fbWidth / 2, 32, new Color(255, 220, 60).getRGB());
        }


        if (createPromptActive) {
            int dialogW = 360, dialogH = 180;
            int dialogX = fbWidth / 2 - dialogW / 2;
            int dialogY = fbHeight / 2 - dialogH / 2;
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(40, 40, 50, 240), dialogX, dialogY, dialogX + dialogW, dialogY + dialogH, 10, 10, 10, 10, 30);
            RenderUtils.renderRoundedOutline(drawContext, new Color(60, 200, 120, 220), dialogX, dialogY, dialogX + dialogW, dialogY + dialogH, 10, 10, 10, 10, 2, 30);
            TextRenderer.drawCenteredString(label(Label.CREATE_CONFIG), drawContext, fbWidth / 2, dialogY + 24, Color.WHITE.getRGB());
            TextRenderer.drawString(label(Label.NAME), drawContext, dialogX + 32, dialogY + 56, Color.WHITE.getRGB());

            int nameFieldX = dialogX + 100;
            int nameFieldY = dialogY + 52;
            int nameFieldW = dialogW - 132;
            int nameFieldH = 24;
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), nameFieldActive ? new Color(40, 80, 120, 255) : new Color(30, 30, 35, 255), nameFieldX, nameFieldY, nameFieldX + nameFieldW, nameFieldY + nameFieldH, 6, 6, 6, 6, 10);
            if (!createPromptName.toString().isEmpty()) {
                TextRenderer.drawString(createPromptName, drawContext, nameFieldX + 8, nameFieldY + 8, Color.WHITE.getRGB());
            }
            TextRenderer.drawString(label(Label.DESCRIPTION), drawContext, dialogX + 32, dialogY + 96, Color.WHITE.getRGB());

            int descFieldX = dialogX + 130;
            int descFieldY = dialogY + 92;
            int descFieldW = dialogW - 162;
            int descFieldH = 24;
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), descFieldActive ? new Color(40, 80, 120, 255) : new Color(30, 30, 35, 255), descFieldX, descFieldY, descFieldX + descFieldW, descFieldY + descFieldH, 6, 6, 6, 6, 10);
            if (!createPromptDesc.toString().isEmpty()) {
                TextRenderer.drawString(createPromptDesc, drawContext, descFieldX + 8, descFieldY + 8, Color.WHITE.getRGB());
            }

            int cbtnW = 100, cbtnH = 32;
            int cbtnX = fbWidth / 2 - cbtnW / 2;
            int cbtnY = dialogY + dialogH - 48;
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(60, 200, 120, 220), cbtnX, cbtnY, cbtnX + cbtnW, cbtnY + cbtnH, 8, 8, 8, 8, 20);
            TextRenderer.drawCenteredString(label(Label.CREATE), drawContext, cbtnX + cbtnW / 2, cbtnY + 8, Color.WHITE.getRGB());
        }

        RenderUtils.scaledProjection();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double scaleFactor = MinecraftClient.getInstance().getWindow().getScaleFactor();
        double mX = mouseX * scaleFactor;
        double mY = mouseY * scaleFactor;

        int fbWidth = MinecraftClient.getInstance().getWindow().getFramebufferWidth();
        int fbHeight = MinecraftClient.getInstance().getWindow().getFramebufferHeight();

        int boxWidth = 600;
        int boxHeight = 400;
        int boxX = fbWidth / 2 - boxWidth / 2;
        int boxY = fbHeight / 2 - boxHeight / 2;

        int tabWidth = 120;
        int tabHeight = 36;
        int tabY = boxY + 16;
        int tabSpacing = 8;
        int tabStartX = boxX + 32;

        int listX = boxX + 32;
        int listY = boxY + 70;
        int entryHeight = 48;
        int entrySpacing = 12;
        List<ConfigEntry> configsToShow = switch (currentTab) {
            case PERSONAL -> personalConfigs;
            case PUBLIC -> publicConfigs;
            case SHARED -> sharedConfigs;
        };


        if (createPromptActive) {
            int dialogW = 360;
            int dialogH = 180;
            int dialogX = fbWidth / 2 - dialogW / 2;
            int dialogY = fbHeight / 2 - dialogH / 2;

            int nameFieldX = dialogX + 100;
            int nameFieldY = dialogY + 52;
            int nameFieldW = dialogW - 132;
            int nameFieldH = 24;

            int descFieldX = dialogX + 130;
            int descFieldY = dialogY + 92;
            int descFieldW = dialogW - 162;
            int descFieldH = 24;

            int cbtnW = 100;
            int cbtnH = 32;
            int cbtnX = fbWidth / 2 - cbtnW / 2;
            int cbtnY = dialogY + dialogH - 48;

            if (mX < dialogX || mX > dialogX + dialogW || mY < dialogY || mY > dialogY + dialogH) {
                createPromptActive = false;
                createPromptName = EncryptedString.of("");
                createPromptDesc = EncryptedString.of("");
                nameFieldActive = true;
                descFieldActive = false;
                return true;
            }

            if (mX >= nameFieldX && mX <= nameFieldX + nameFieldW && mY >= nameFieldY && mY <= nameFieldY + nameFieldH) {
                nameFieldActive = true;
                descFieldActive = false;
                return true;
            }
            if (mX >= descFieldX && mX <= descFieldX + descFieldW && mY >= descFieldY && mY <= descFieldY + descFieldH) {
                nameFieldActive = false;
                descFieldActive = true;
                return true;
            }
            if (mX >= cbtnX && mX <= cbtnX + cbtnW && mY >= cbtnY && mY <= cbtnY + cbtnH) {

                String configData = taz.womp.Womp.INSTANCE.getConfigManager().getCurrentConfigString();
                String result = CreateConfig.run(createPromptName.toString(), configData, createPromptDesc.toString(), 0);
                showFeedback(result);
                createPromptActive = false;
                createPromptName = EncryptedString.of("");
                createPromptDesc = EncryptedString.of("");
                nameFieldActive = true;
                descFieldActive = false;
                refreshConfigs();
                return true;
            }

            return true;
        }
        if (sharePromptActive) {
            int dialogW = 320;
            int dialogH = 120 + 32 + 28 * Math.min(MAX_VISIBLE_SHARED_USERS, shareDialogUsers.size());
            int dialogX = fbWidth / 2 - dialogW / 2;
            int dialogY = fbHeight / 2 - dialogH / 2;
            int shareBtnW = 80, shareBtnH = 28;
            int shareBtnX = dialogX + dialogW / 2 - shareBtnW / 2;
            int shareBtnY = dialogY + 104;
            int userListY = shareBtnY + shareBtnH + 8;
            int userH = 28;
            int visible = Math.min(MAX_VISIBLE_SHARED_USERS, shareDialogUsers.size());

            if (mX < dialogX || mX > dialogX + dialogW || mY < dialogY || mY > dialogY + dialogH) {
                sharePromptActive = false;
                sharePromptInput = null;
                selectedShareIndex = -1;
                return true;
            }

            if (mX >= shareBtnX && mX <= shareBtnX + shareBtnW && mY >= shareBtnY && mY <= shareBtnY + shareBtnH) {
                if (selectedShareIndex >= 0 && selectedShareIndex < personalConfigs.size()) {
                    onShareConfig(personalConfigs.get(selectedShareIndex), sharePromptInput == null ? "" : sharePromptInput.toString());
                }
                sharePromptActive = false;
                sharePromptInput = null;
                selectedShareIndex = -1;
                return true;
            }

            for (int i = 0; i < visible; i++) {
                int idx = i + shareUserScroll;
                if (idx >= shareDialogUsers.size()) break;
                EncryptedString user = shareDialogUsers.get(idx);
                int userY = userListY + i * userH;
                int xBtnW = 20, xBtnH = 20;
                int xBtnX = dialogX + dialogW - 32 - xBtnW - 6;
                int xBtnY = userY + (userH - xBtnH) / 2;
                if (mX >= xBtnX && mX <= xBtnX + xBtnW && mY >= xBtnY && mY <= xBtnY + xBtnH) {

                    String configName = personalConfigs.get(selectedShareIndex).name.toString();
                    String myName = taz.womp.Womp.INSTANCE.getSession().getUsername();
                    if (user.toString().equals(myName)) {
                        UnshareConfig.run(configName);
                    } else {
                        UnshareConfig.run(configName, user.toString());
                    }
                    List<String> users = ShareConfig.getSharedUsers(configName);
                    shareDialogUsers = new ArrayList<>();
                    for (String u : users) shareDialogUsers.add(EncryptedString.of(u));
                    return true;
                }
            }

            return true;
        }

        for (Tab tab : Tab.values()) {
            int tabX = tabStartX + tab.ordinal() * (tabWidth + tabSpacing);
            if (mX >= tabX && mX <= tabX + tabWidth && mY >= tabY && mY <= tabY + tabHeight) {
                currentTab = tab;
                return true;
            }
        }

        if (currentTab == Tab.PERSONAL) {
            int btnW = 140, btnH = 36;
            int btnX = boxX + boxWidth - btnW - 32;
            int btnY = boxY + 16;
            if (mX >= btnX && mX <= btnX + btnW && mY >= btnY && mY <= btnY + btnH) {
                createPromptActive = true;
                createPromptName = EncryptedString.of("");
                createPromptDesc = EncryptedString.of("");
                return true;
            }
        }

        if (configsToShow.isEmpty() && currentTab == Tab.PERSONAL) {
            int btnW = 200, btnH = 48;
            int btnX = boxX + boxWidth / 2 - btnW / 2;
            int btnY = listY + 80;
            if (mX >= btnX && mX <= btnX + btnW && mY >= btnY && mY <= btnY + btnH) {
                createPromptActive = true;
                createPromptName = EncryptedString.of("");
                createPromptDesc = EncryptedString.of("");
                return true;
            }
        }

        for (int i = 0; i < configsToShow.size(); i++) {
            ConfigEntry entry = configsToShow.get(i);
            int entryY = listY + i * (entryHeight + entrySpacing);

            int btnW = 60;
            int btnH = 28;
            int btnY = entryY + (entryHeight - btnH) / 2;
            int spacing = 5;
            int entryRightX = listX + boxWidth - 64;
            int buttonRightMargin = 16;

            boolean isPersonal = currentTab == Tab.PERSONAL;

            int shareBtnX = 0, deleteBtnX = 0, saveBtnX = 0, loadBtnX = 0;

            if (isPersonal) {

                if (isPersonal && !entry.isPublic) {
                    shareBtnX = entryRightX - buttonRightMargin - btnW;
                    deleteBtnX = shareBtnX - spacing - btnW;
                    saveBtnX = deleteBtnX - spacing - btnW;
                    loadBtnX = saveBtnX - spacing - btnW;
                } else {
                    shareBtnX = entryRightX - buttonRightMargin - btnW;
                    deleteBtnX = shareBtnX - spacing - btnW;
                    saveBtnX = deleteBtnX - spacing - btnW;
                    loadBtnX = saveBtnX - spacing - btnW;
                }
            } else {
                shareBtnX = entryRightX - buttonRightMargin - btnW;
                deleteBtnX = shareBtnX - spacing - btnW;
                loadBtnX = deleteBtnX - spacing - btnW;
            }


            if (mX >= loadBtnX && mX <= loadBtnX + btnW && mY >= btnY && mY <= btnY + btnH) {
                onLoadConfig(entry);
                return true;
            }


            if (isPersonal) {
                if (mX >= saveBtnX && mX <= saveBtnX + btnW && mY >= btnY && mY <= btnY + btnH) {
                    onSaveConfig(entry);
                    return true;
                }
            }


            if (mX >= deleteBtnX && mX <= deleteBtnX + btnW && mY >= btnY && mY <= btnY + btnH) {
                onDeleteConfig(entry);
                return true;
            }


            if (mX >= shareBtnX && mX <= shareBtnX + btnW && mY >= btnY && mY <= mY + btnH) {
                selectedShareIndex = i;
                sharePromptActive = true;
                sharePromptInput = EncryptedString.of("");
                return true;
            }


        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onLoadConfig(ConfigEntry entry) {
        GetConfigData.ConfigDataResult result = GetConfigData.run(entry.name.toString());
        if (result == null || result.configData == null || (result.configData.startsWith("Internal error"))) {
            showFeedback("Failed to load config");
            return;
        }
        try {
            ConfigManager manager = taz.womp.Womp.INSTANCE.getConfigManager();
            manager.loadFromString(result.configData);
            showFeedback("Config loaded");

        } catch (Exception e) {
            showFeedback("Error applying config");
        }
        refreshConfigs();
    }
    private void onDeleteConfig(ConfigEntry entry) {
        String result = DeleteConfig.run(entry.name.toString());
        showFeedback(result);
        refreshConfigs();
    }
    private void onShareConfig(ConfigEntry entry, String username) {
        String result = ShareConfig.run(entry.name.toString(), username);
        showFeedback(result);
        refreshConfigs();
    }

    private void onSaveConfig(ConfigEntry entry) {
        String configData = taz.womp.Womp.INSTANCE.getConfigManager().getCurrentConfigString();
        String result = UpdateConfig.run(entry.name.toString(), configData);
        showFeedback(result);
        refreshConfigs();
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (sharePromptActive) {
            if (chr == '\n' || chr == '\r') {
                if (selectedShareIndex >= 0 && selectedShareIndex < personalConfigs.size()) {
                    onShareConfig(personalConfigs.get(selectedShareIndex), sharePromptInput == null ? "" : sharePromptInput.toString());
                }
                sharePromptActive = false;
                sharePromptInput = null;
                selectedShareIndex = -1;
                return true;
            } else if (chr == '\b' || chr == 127) {
                if (sharePromptInput != null && sharePromptInput.length() > 0) {
                    sharePromptInput = EncryptedString.of(sharePromptInput.toString().substring(0, sharePromptInput.length() - 1));
                }
                return true;
            } else if (chr >= 32 && chr < 127) {
                if (sharePromptInput == null) sharePromptInput = EncryptedString.of("");
                sharePromptInput = EncryptedString.of(sharePromptInput.toString() + chr);
                return true;
            }
        }
        if (createPromptActive) {
            if (nameFieldActive) {
                if (chr == '\n' || chr == '\r') {

                    return true;
                } else if ((chr == '\b' || chr == 127) && createPromptName.length() > 0) {
                    createPromptName = EncryptedString.of(createPromptName.toString().substring(0, createPromptName.length() - 1));
                    return true;
                } else if (chr >= 32 && chr < 127 && createPromptName.length() < 16) {
                    createPromptName = EncryptedString.of(createPromptName.toString() + chr);
                    return true;
                }
            }
            else if (descFieldActive) {
                if (chr == '\n' || chr == '\r') {

                    return true;
                } else if ((chr == '\b' || chr == 127) && createPromptDesc.length() > 0) {
                    createPromptDesc = EncryptedString.of(createPromptDesc.toString().substring(0, createPromptDesc.length() - 1));
                    return true;
                } else if (chr >= 32 && chr < 127 && createPromptDesc.length() < 32) {
                    createPromptDesc = EncryptedString.of(createPromptDesc.toString() + chr);
                    return true;
                }
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (sharePromptActive) {
            if (keyCode == 256) { // ESC
                sharePromptActive = false;
                sharePromptInput = null;
                selectedShareIndex = -1;
                return true;
            }

            if ((keyCode == 259 || keyCode == 261)) {
                if (sharePromptInput != null && sharePromptInput.length() > 0) {
                    sharePromptInput = EncryptedString.of(sharePromptInput.toString().substring(0, sharePromptInput.length() - 1));
                }
                return true;
            }
            return false;
        }
        if (createPromptActive) {

            if ((keyCode == 259 || keyCode == 261)) {
                if (nameFieldActive && createPromptName.length() > 0) {
                    createPromptName = EncryptedString.of(createPromptName.toString().substring(0, createPromptName.length() - 1));
                    return true;
                } else if (descFieldActive && createPromptDesc.length() > 0) {
                    createPromptDesc = EncryptedString.of(createPromptDesc.toString().substring(0, createPromptDesc.length() - 1));
                    return true;
                }
            }

            if (keyCode == 258) {
                if (nameFieldActive) {
                    nameFieldActive = false;
                    descFieldActive = true;
                } else {
                    nameFieldActive = true;
                    descFieldActive = false;
                }
                return true;
            }

            if (keyCode == 256) {
                createPromptActive = false;
                createPromptName = EncryptedString.of("");
                createPromptDesc = EncryptedString.of("");
                nameFieldActive = true;
                descFieldActive = false;
                return true;
            }
            return false;
        }
        if (keyCode == 256) { // ESC
            MinecraftClient.getInstance().setScreen(new ClickGUI());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
} 