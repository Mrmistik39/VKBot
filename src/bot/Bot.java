package bot;

import ai.api.AIConfiguration;
import ai.api.AIDataService;
import bot.handler.LongPollHandler;
import bot.handler.MessageReplier;
import bot.tasks.*;
import bot.utils.Pair;
import com.google.maps.GeoApiContext;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.messages.LongpollParams;
import com.vk.api.sdk.objects.users.UserXtrCounters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class Bot {
    private String AI_CLIENT_KEY, GOOGLE_KEY,ACCESS_TOKEN, WEATHER_KEY;
    private int USER_ID_MAIN;
    private int[] MEME_RESOURCES;

    public static final Logger logger= LoggerFactory.getLogger(Bot.class);
    private Map<String,String> emojies;
    private AIDataService dataService;
    private LongPollHandler handler;

    private MainApiInteracter interacter;
    private LikesCounter counter;
    private BitcoinRate bitcoinRate;
    private WeatherForecast forecast;
    private DistanceCounter distanceCounter;
    private RandomImage randomImage;
    private RandomVkItem randomItem;
    private TextConverter converter;
    private LinkedHashMap<Integer,GuessNumber> guessGame;

    private HashSet<Integer> ignored;

    private String userStatus;

    private AtomicInteger countOfInteractions;

    public Bot() {
        loadConfig();
        initBot();
    }

    public static void main(String[] args) {
        new Bot();
    }
    private void loadConfig(){
        try {
            Properties properties=new Properties();
            properties.load(Bot.class.getClassLoader().getResourceAsStream("config.properties"));
            AI_CLIENT_KEY =(String)properties.get("ai-client-key");
            WEATHER_KEY =(String)properties.get("weather-key");
            GOOGLE_KEY =(String)properties.get("google-key");
            ACCESS_TOKEN=(String)properties.get("access-token");
            USER_ID_MAIN=Integer.valueOf((String)properties.get("user-id-main"));
            MEME_RESOURCES=new int[]{Integer.valueOf((String)properties.get("meme-4ch")),
                    Integer.valueOf((String)properties.get("meme-mdk")),
                    Integer.valueOf((String)properties.get("meme-9gag"))};
        } catch (IOException e) {
            logger.error("Initialize error (can`t load properties).");
        }
    }
    private void initEmojies(){
        emojies=new HashMap<>();
        emojies.put("cuteSmile","&#128522;");
        emojies.put("laughingSmile","&#128514;");
        emojies.put("coolEmoji","&#128526;");
        emojies.put("angryEmoji","&#128545;");
        emojies.put("heart","&#10084;");
        emojies.put("dollar","&#128181;");
        emojies.put("fuck","&#128405;");
        emojies.put("sun","&#9728;");
        emojies.put("clWithLight","&#9928;");
        emojies.put("snowflake","&#10052;");
        emojies.put("cloud","&#9729;");
        emojies.put("clWithSnow","&#127784;");
        emojies.put("clWithRain","&#127783;");
        emojies.put("clWithSun","&#9925;");
        emojies.put("thermometer","&#127777;");
        emojies.put("sunWithCl","&#127780;");
        emojies.put("sunWithClWithR","&#127782;");
        emojies.put("lightning","&#127785;");
        emojies.put("subway","&#9410;");
        emojies.put("watch","&#8986;");
        emojies.put("photo","&#127750;");
        emojies.put("mail","&#128236;");
        emojies.put("camera","&#128249;");
        emojies.put("exclamation","&#10071;");
    }
    private void initTasks(VkApiClient vk,UserActor user){
        interacter =new MainApiInteracter(user,vk);
        counter=new LikesCounter(user,vk);
        bitcoinRate =new BitcoinRate();
        forecast=new WeatherForecast(WEATHER_KEY);
        distanceCounter=new DistanceCounter(new GeoApiContext.Builder()
                .apiKey(GOOGLE_KEY)
                .build());
        randomImage=new RandomImage();
        randomItem =new RandomVkItem(user,vk,MEME_RESOURCES);
        converter=new TextConverter();
    }
    private void initAi(){
        dataService=new AIDataService(new AIConfiguration(AI_CLIENT_KEY));
    }
    private void initBot(){
      ignored=new HashSet<>();
      guessGame=new LinkedHashMap<>();
      countOfInteractions=new AtomicInteger(0);
      VkApiClient vk=new VkApiClient(new HttpTransportClient());
      UserActor user =new UserActor(USER_ID_MAIN,ACCESS_TOKEN);
      initTasks(vk,user);
      initAi();
      initEmojies();
      initLongPollHandler(user.getId());
      userStatus=interacter.getStatus();
      addShutdownHook();
      startCheckThread();
      interacter.setStatus("VkBot is working now (there is no human user).");
      interacter.setOnline(true);
      interacter.sendMessageToOwner("VkBot has been started on:\nserverTime["+new Date().toString()+"]\nHello!");
      System.out.println("\n╔╗╔╦═══╦╗─╔╗─╔══╦╗\n" +
                "║║║║╔══╣║─║║─║╔╗║║\n" +
                "║╚╝║╚══╣║─║║─║║║║║\n" +
                "║╔╗║╔══╣║─║║─║║║╠╝\n" +
                "║║║║╚══╣╚═╣╚═╣╚╝╠╗\n" +
                "╚╝╚╩═══╩══╩══╩══╩╝");
    }
    private void initLongPollHandler(int userId){
        handler=new LongPollHandler(this,userId,new MessageReplier(this,emojies));
        handler.start();
    }



    public void sendMessage(int id, String text){
       check();
       interacter.sendMessage(id, text);
    }
    public void sendMessageWithPhoto(int id, String text, File photo){
        check();
        interacter.sendMessageWithPhoto(id, text, photo);
    }
    public void sendMessageWithPhoto(int id, String text,String...photo){
        check();
        interacter.sendMessageWithPhoto(id, text, photo);
    }
    public void sendMessageWithVideo(int id,String text,String video){
        check();
        interacter.sendMessageWithVideo(id, text, video);
    }
    public int calculateCountOfLikes(UserXtrCounters target,String albumId){
        check();
        return counter.calculateCountOfLikes(target, albumId);
    }
    public int calculateContOfLikesOnPosts(UserXtrCounters target){
        check();
        return counter.calculateContOfLikesOnPosts(target);
    }
    public String[] bitcoinRate(){
        return bitcoinRate.bitcoinRate();
    }
    public String receiveWeatherForecast(String city,String countryCode){
        return forecast.receiveWeatherForecast(city, countryCode);
    }
    public String calculateTimeInSubway(String city,String origin,String destination){
        return distanceCounter.calculateTimeInSubway(city, origin, destination);
    }
    public File randomImage() {
        return randomImage.randomImage();
    }
    public Pair<String, String[]> randomMeme(){
        check();
        Pair<String,String[]> pair=randomItem.randomMeme();
        pair=pair.getValue().length>0?pair:randomMeme();
        return pair;
    }
    public String randomVideo(){
        check();
        return randomItem.randomVideo();
    }
    public String textToEmoji(char[] text,String background,String foreground){
        return converter.textToEmoji(text, background, foreground);
    }
    public synchronized boolean isPlaying(int id){
        return guessGame.containsKey(id);
    }
    public synchronized void startNewGame(int id){
        guessGame.put(id,new GuessNumber());
    }
    public synchronized void endGame(int id){
        guessGame.remove(id);
    }


    public synchronized void interruptLongPoll(){
        handler.setShouldReact(false);
        System.out.println("Long Poll interrupted.");
    }
    public synchronized void startLongPoll(){
        handler.setShouldReact(true);
        System.out.println("Long Poll started.");
    }
    public synchronized void ignore(int id){
        ignored.add(id);
    }
    public synchronized void unignore(int id){
        ignored.remove(id);
    }
    public synchronized boolean isIgnored(int id){
        return ignored.contains(id);
    }


    public LongpollParams getLongpollParams(){
        return interacter.getLongpollParams();
    }
    public UserXtrCounters getAddressee(String id){
        check();
        return interacter.getAddressee(id);
    }
    public AIDataService getDataService() {
        return dataService;
    }
    public GuessNumber getGuessGame(int id){
        return guessGame.get(id);
    }
    public void exit(int status){
        System.exit(status);
    }
    private void addShutdownHook(){
        Runtime.getRuntime().addShutdownHook(new Thread(()->{

            interacter.sendMessageToOwner("VkBot has been exited on:\nserverTime["
                    + new Date().toString() + "]\nBye-bye!");
            interacter.setStatus(userStatus);
            interacter.setOnline(false);
            System.out.println("╔══╗╔╗╔╦═══╗──╔══╗╔╗╔╦═══╦╗\n" +
                    "║╔╗║║║║║╔══╝──║╔╗║║║║║╔══╣║\n" +
                    "║╚╝╚╣╚╝║╚══╦══╣╚╝╚╣╚╝║╚══╣║\n" +
                    "║╔═╗╠═╗║╔══╩══╣╔═╗╠═╗║╔══╩╝\n" +
                    "║╚═╝║╔╝║╚══╗──║╚═╝║╔╝║╚══╦╗\n" +
                    "╚═══╝╚═╩═══╝──╚═══╝╚═╩═══╩╝\n");

        }));
    }
    private void startCheckThread(){
        Thread thread=new Thread(()->{
            while (Thread.currentThread().isAlive()) {
                try {
                    Thread.sleep(1000);
                    countOfInteractions.set(0);
                } catch (InterruptedException ignore) {}
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    private void check(){
        while (countOfInteractions.get()>=3) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {}
        }
        countOfInteractions.getAndIncrement();
    }

}
