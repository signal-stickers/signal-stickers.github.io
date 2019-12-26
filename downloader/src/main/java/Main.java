import com.squareup.moshi.Moshi;
import json.InputPack;
import json.InputPackList;
import json.OutputPack;
import json.OutputPackList;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.util.Hex;
import org.whispersystems.libsignal.util.Pair;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.messages.SignalServiceStickerManifest;
import org.whispersystems.signalservice.api.push.TrustStore;
import org.whispersystems.signalservice.internal.configuration.SignalCdnUrl;
import org.whispersystems.signalservice.internal.configuration.SignalContactDiscoveryUrl;
import org.whispersystems.signalservice.internal.configuration.SignalServiceConfiguration;
import org.whispersystems.signalservice.internal.configuration.SignalServiceUrl;
import org.whispersystems.signalservice.internal.util.Util;

import java.io.*;
import java.security.Security;
import java.util.UUID;

class Main {

  public static void main(String[] args) throws IOException, InvalidMessageException {
    Security.addProvider(new BouncyCastleProvider());

    Moshi                        moshi    = new Moshi.Builder().build();
    SignalServiceMessageReceiver receiver = buildMessageReceiver();
    InputPackList                packList = readLocalPackList(moshi);
    OutputPackList               output   = new OutputPackList();

    for (int i = 0; i < packList.packs.size(); i++) {
      System.out.println("Processing " + (i+1) + "/" + packList.packs.size());

      InputPack                    pack          = packList.packs.get(i);
      Pair<String, String>         idAndKey      = parseUrl(pack.url);
      byte[]                       packId        = Hex.fromStringCondensed(idAndKey.first());
      byte[]                       packKey       = Hex.fromStringCondensed(idAndKey.second());
      SignalServiceStickerManifest manifest      = receiver.retrieveStickerManifest(packId, packKey);
      String                       coverFilename = idAndKey.first() + ".webp";

      downloadCover(receiver, coverFilename, packId, packKey, manifest.getCover().get().getId());
      output.packs.add(new OutputPack(manifest.getTitle().or(""),
                                      manifest.getAuthor().or(""),
                                      pack.tags,
                                      pack.url,
                                      coverFilename));

      writePacksToDisk(moshi, output);
    }

    System.exit(0);
  }

  private static InputPackList readLocalPackList(Moshi moshi) throws IOException {
    InputStream is = new FileInputStream("../packs.json");
    return moshi.adapter(InputPackList.class).fromJson(Util.readFully(is));
  }

  private static void downloadCover(SignalServiceMessageReceiver receiver, String filename, byte[] packId, byte[] packKey, int stickerId)
      throws IOException, InvalidMessageException
  {
    InputStream      is   = receiver.retrieveSticker(packId, packKey, stickerId);
    FileOutputStream faos = new FileOutputStream("../img/covers/" + filename);

    Util.copy(is, faos);
  }

  private static void writePacksToDisk(Moshi moshi, OutputPackList packList) throws IOException {
    PrintStream disk = new PrintStream(new FileOutputStream("../data.json"), true);
    disk.println(moshi.adapter(OutputPackList.class).indent("  ").toJson(packList));
    disk.close();
  }

  private static Pair<String, String> parseUrl(String url) {
    return new Pair<>(url.substring(40, 72), url.substring(82));
  }

  private static SignalServiceMessageReceiver buildMessageReceiver() {
    return new SignalServiceMessageReceiver(buildServiceConfiguration(),
                                            UUID.randomUUID(),
                                            "",
                                            "",
                                            null,
                                            "owa",
                                            null,
                                            null);
  }

  private static SignalServiceConfiguration buildServiceConfiguration() {
    TrustStore trustStore = new SignalServiceTrustStore();
    return new SignalServiceConfiguration(new SignalServiceUrl[] { new SignalServiceUrl("https://textsecure-service.whispersystems.org", trustStore) },
                                          new SignalCdnUrl[] { new SignalCdnUrl("https://cdn.signal.org", trustStore) },
                                          new SignalContactDiscoveryUrl[] { new SignalContactDiscoveryUrl("https://api.directory.signal.org", trustStore) });
  }

  private static class SignalServiceTrustStore implements TrustStore {
    @Override
    public InputStream getKeyStoreInputStream() {
      try {
        return new FileInputStream("whisper.store");
      } catch (FileNotFoundException e) {
        throw new AssertionError(e);
      }
    }

    @Override
    public String getKeyStorePassword() {
      return "whisper";
    }
  }
}
