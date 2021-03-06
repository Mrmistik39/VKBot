package bot.handler;


import bot.Bot;
import bot.handler.model.DistanceDataModel;
import bot.handler.model.EmojiDataModel;
import bot.handler.model.WeatherDataModel;
import bot.utils.Pair;
import com.vk.api.sdk.objects.users.UserXtrCounters;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for parse and reply to messages.
 */
public class MessageReplier {
    private Bot bot;
    private Map<String, String> emojies;
    private HashMap<Integer,WeatherDataModel> weatherQueries;
    private HashMap<Integer,DistanceDataModel> distanceQueries;
    private HashMap<Integer,EmojiDataModel> emojiQuery;

    public MessageReplier(Bot bot, Map<String,String> emojies) {
        this.bot = bot;
        this.emojies=emojies;
        weatherQueries=new HashMap<>();
        distanceQueries=new HashMap<>();
        emojiQuery=new HashMap<>();
    }

    public void detectQueryOrParse(String message, UserXtrCounters sender){
        int id=sender.getId();

        if (!bot.isMember(id)){
            bot.sendMessage(id,"Чтобы получить доступ к функционалу бота, необходимо подписаться. " +
                    "Заранее спасибо)");
            return;
        }

        WeatherDataModel weatherDataModel=weatherQueries.get(id);
        DistanceDataModel distanceDataModel=distanceQueries.get(id);
        EmojiDataModel emojiDataModel=emojiQuery.get(id);
        if (weatherDataModel!=null) doWeatherQuery(id,message,weatherDataModel);
        else if (distanceDataModel!=null) doDistanceQuery(id,message,distanceDataModel);
        else if (emojiDataModel!=null) doEmojiQuery(id,message,emojiDataModel);
        else parse(message, sender);
    }

    private void doWeatherQuery(Integer id, String message, WeatherDataModel weatherDataModel){
        switch (weatherDataModel.whatIsNext()){
            case "city":
                weatherDataModel.setCity(message);
                bot.sendMessage(id,"Введите код страны (например, ru):");
                break;
            case "countryCode":
                String emojiLine= emojies.get("thermometer")+ emojies.get("sun")
                        + emojies.get("clWithLight")+ emojies.get("lightning")
                        + emojies.get("snowflake")+ emojies.get("cloud")
                        + emojies.get("clWithSnow")+ emojies.get("clWithRain")
                        + emojies.get("clWithSun")+ emojies.get("sunWithCl")
                        + emojies.get("sunWithClWithR");
                weatherDataModel.setCountryCode(message);
                String result=bot.receiveWeatherForecast(weatherDataModel.getCity(),weatherDataModel.getCountryCode());
                bot.sendMessage(id,result.equals("")?"Некорректный ввод.":emojiLine+
                        "\n\n"+result+"\n\n"+emojiLine);
                weatherQueries.remove(id);
                break;

        }
    }

    private void doDistanceQuery(Integer id, String message, DistanceDataModel distanceDataModel){
        switch (distanceDataModel.whatIsNext()){
            case "city":
                distanceDataModel.setCity(message);
                bot.sendMessage(id, "Введите станцию отправления:");
                break;
            case "origin":
                distanceDataModel.setOrigin(message);
                bot.sendMessage(id,"Введите станцию прибытия:");
                break;
            case "destination":
                distanceDataModel.setDestination(message);
                String result=bot.calculateTimeInSubway(distanceDataModel.getCity(),distanceDataModel.getOrigin(),
                        distanceDataModel.getDestination());
                bot.sendMessage(id,result.equals("")?"Некорректный ввод. Ввод должен быть в" +
                        " формате: \"Метро: <город>, <начальная станция>, <конечная станция>\"":result+ emojies
                        .get("watch"));
                distanceQueries.remove(id);
                break;
        }
    }

    private void doEmojiQuery(Integer id, String message, EmojiDataModel emojiDataModel){
        switch (emojiDataModel.whatIsNext()){
            case "text":
                emojiDataModel.setText(message.toLowerCase());
                bot.sendMessage(id,"Введите эмодзи заднего фона:");
                break;
            case "background":
                emojiDataModel.setBackground(message);
                bot.sendMessage(id,"Введите эмодзи самого текста:");
                break;
            case "foreground":
                emojiDataModel.setForeground(message);
                String result=bot.textToEmoji(emojiDataModel.getText().toCharArray(),emojiDataModel.getBackground(),
                        emojiDataModel.getForeground());
                bot.sendMessage(id,result.equals("")?"Я не могу написать это с помощью эмодзи. " +
                        "Попробуйте что-нибудь другое.":result);
                emojiQuery.remove(id);
                break;

        }
    }

    /**
     * Parse and reply to message from user.
     * @param message user message
     * @param sender user who send message
     * Functions:
     * @see Bot#sendMessage(int, String)
     * @see Bot#sendMessageWithPhoto(int, String, File)
     * @see Bot#sendMessageWithPhoto(int, String, String...)
     * @see Bot#sendMessageWithVideo(int, String, String)
     * @see Bot#calculateCountOfLikes(UserXtrCounters, String)
     * @see Bot#calculateContOfLikesOnPosts(UserXtrCounters)
     * @see Bot#bitcoinRate()
     * @see Bot#startNewGame(int)
     * @see Bot#randomImage()
     * @see Bot#randomMeme()
     * @see Bot#randomVideo()
     * @see Bot#textToEmoji(char[], String, String)
     * @see Bot#receiveWeatherForecast(String, String)
     * @see Bot#calculateTimeInSubway(String, String, String)
     * @see Bot#aiAnswer(String)
     */
     private void parse(String message, UserXtrCounters sender){
        String data=message.toLowerCase();
        int id = sender.getId();

        if(data.matches("здравствуй.*|привет.*")){

            String heart= emojies.get("heart");
            bot.sendMessage(id,
                    "Здравствуй, "+sender.getFirstName()+" "+sender.getLastName()+". "+
                    "Меня зовут Бот Юджин"+ emojies.get("coolEmoji")+"\n\n" +
                    "Вот что я пока могу:\n\n" +

                    "1.\"Лайки на стене\": пришлю суммарное количество лайков " +
                    "под последними 100 фото со стены" +heart+"\n\n"+

                    "2.\"Лайки в профиле\": пришлю суммарное количество лайков " +
                    "под последними 100 фото в профиле" +heart+"\n\n"+

                    "3.\"Всего лайков\": пришлю суммарное количество лайков " +
                    "под последними 100 записями" +heart+"\n\n"+

                    "4.\"Курс биткоина\": курс биткоина в долларах" + emojies.get("dollar")+"\n\n" +

                    "5.\"Погода\": пришлю прогноз погоды"+ emojies.get("thermometer")+"\n\n" +

                    "6.\"Поиграем\": я загадаю число от 0 до 100, а тебе нужно" +
                    " будет угадать, пользуясь тремя командами: \">(число)\" больше, \"<(число)\" меньше" +
                    ", \"(число)\".\n\n"+

                    "7. \"Метро\": покажу время пути в метро"+ emojies.get("subway")+"\n\n"+

                    "8.\"Случайное фото\": пришлю случайное фото"+ emojies.get("photo")+"\n\n"+

                    "9.\"Скинь мем\": скину случайный мем"+ emojies.get("mail")+"\n\n"+

                    "10.\"Скинь видео\": скину случайное видео"+ emojies.get("camera")+"\n\n"+

                    "11.\"Эмодзи\": напишу текст эмодзи"+ emojies.get("exclamation")+emojies.get("exclamation")
                    + emojies.get("exclamation")+ "Двойные эмодзи могут вызывать проблемы. Также стоит обратить" +
                    " внимание на длину сообщения, т.к. в ВК есть ограничение на нее" + emojies.get("exclamation")
                    + emojies.get("exclamation")+ emojies.get("exclamation")+"\n\n"+

                    "12.\"Пошли меня\": могу послать"+ emojies.get("fuck")+"\n\n"+

                    "Или можем просто поболтать, но я еще учусь, и мои ответы могут быть не совсем точными.\n"+
                    "Знаю, пока это немного, но я развиваюсь"+ emojies.get("cuteSmile"));

        } else if(data.matches("лайки на стене.*|1")){

            bot.sendMessage(id,""+sender.getFirstName()+" "+sender.getLastName()+", " +
                    "у тебя "+bot.calculateCountOfLikes(sender,"wall")+ emojies.get("heart")+" лайков под " +
                    "фотографиями на стене.");

        } else if(data.matches("лайки в профиле.*|2")){

            bot.sendMessage(id,""+sender.getFirstName()+" "+sender.getLastName()+", " +
                    "у тебя "+bot.calculateCountOfLikes(sender,"profile")+ emojies.get("heart")+" лайков " +
                    "под фотографиями в профиле.");

        } else if(data.matches("всего лайков.*|3")){

            bot.sendMessage(id,""+sender.getFirstName()+" "+sender.getLastName()+", " +
                    "у тебя "+bot.calculateContOfLikesOnPosts(sender)+ emojies.get("heart")+" лайков " +
                    "под записями на стене.");

        } else if(data.matches("курс биткоина.*|4")){

            String[] btrate=bot.bitcoinRate();
            String dollar= emojies.get("dollar");
            bot.sendMessage(id,"Курс: "+btrate[0]+dollar+"\n"+
                    "Покупка: "+btrate[1]+dollar+"\n"+
                    "Продажа: "+btrate[2]+dollar);

        } else if(data.matches("погода.*|5")){

            weatherQueries.put(id,new WeatherDataModel());
            bot.sendMessage(id,"Введите город:");

        } else if(data.matches("поиграем.*|6")){

            bot.startNewGame(id);
            bot.sendMessage(id,"Число загадано и игра началась!");

        } else if(data.matches("метро.*|7")){

            distanceQueries.put(id,new DistanceDataModel());
            bot.sendMessage(id,"Введите город:");

        } else if(data.matches("случайное фото.*|8")){

            bot.sendMessage(id,"Минутку...");
            bot.sendMessageWithPhoto(id,"Держи!",bot.randomImage());

        } else if(data.matches("скинь мем.*|9")){

            bot.sendMessage(id,"Минутку...");
            Pair<String,String[]> meme=bot.randomMeme();
            bot.sendMessageWithPhoto(id,meme.getKey(),meme.getValue());

        } else if(data.matches("скинь видео.*|10")){

            bot.sendMessage(id,"Минутку...");
            bot.sendMessageWithVideo(id,"",bot.randomVideo());

        } else if(data.matches("эмодзи.*|11")){

            emojiQuery.put(id,new EmojiDataModel());
            bot.sendMessage(id,"Введите текст, который хотите написать:");

        } else if(data.matches("пошли меня.*|12")){

            bot.sendMessage(id,"Я, конечно, культурный бот, но раз ты просишь...\n" +
                    sender.getFirstName()+" "+sender.getLastName()+", иди нахер"+ emojies.get("fuck"));

        }  else {

            String response=bot.aiAnswer(message);
            bot.sendMessage(id,response.equals("")?"Извини, я тебя не понял.":response);

        }
    }

    /**
     * If user is playing game for parse use this method.
     * @param message user message
     * @param sender user who send message
     * Functions:
     * @see Bot#startNewGame(int)
     * @see Bot#endGame(int)
     */
    public void parseGame(String message, UserXtrCounters sender){
        String data=message.toLowerCase();
        int id = sender.getId();

        if(data.equals("хватит")) {

            bot.endGame(id);
            bot.sendMessage(id,"Спасибо за игру!");

        } else if(data.equals("заново")){

            bot.startNewGame(id);
            bot.sendMessage(id,"Число загадано и игра началась!");

        } else if(data.matches("&gt;\\d*")){

            try {
                boolean isCorrect=bot.checkStatement(id, '>',Integer.valueOf(data.substring(4)));
                bot.sendMessage(id,isCorrect?"Да":"Нет");
            } catch (NumberFormatException e) {
                bot.sendMessage(id,"Некорректный ввод. Ввод должен быть" +
                        " в формате \"<операция><число>\" или \"<число>\"");
            }

        } else if(data.matches("&lt;\\d*")){

            try {
                boolean isCorrect=bot.checkStatement(id, '<',Integer.valueOf(data.substring(4)));
                bot.sendMessage(id,isCorrect?"Да":"Нет");
            } catch (NumberFormatException e) {
                bot.sendMessage(id,"Некорректный ввод. Ввод должен быть" +
                        " в формате \"<операция><число>\" или \"<число>\"");
            }

        } else {

            try {
                boolean isFemale= sender.getSex() != null && sender.getSex().getValue() == 1;
                boolean isCorrect=bot.checkNumber(id,Integer.valueOf(data));
                bot.sendMessage(id,isCorrect?"Ура! Ты "+
                        (isFemale?"угадала":"угадал")+" за "
                        +bot.countOfTryings(id)+" попыток!" +
                        " Спасибо за игру.":"Нет");
                if (isCorrect) bot.endGame(id);
            } catch (NumberFormatException e) {
                bot.sendMessage(id,"Некорректный ввод. Ввод должен быть" +
                        " в формате \"<операция><число>\" или \"<число>\"");
            }

        }
    }

    /**
     * Parse user message to himself.
     * @param message user message
     * @param sender user who send message
     * Functions:
     * @see Bot#sendMessage(int, String)
     * @see Bot#calculateCountOfLikes(UserXtrCounters, String)
     * @see Bot#calculateContOfLikesOnPosts(UserXtrCounters)
     * @see Bot#bitcoinRate()
     * @see Bot#receiveWeatherForecast(String, String)
     * @see Bot#calculateTimeInSubway(String, String, String)
     * @see Bot#aiAnswer(String)
     * @see Bot#interruptLongPoll()
     * @see Bot#startLongPoll()
     * @see Bot#exit(int)
     */
    public void parseAdmin(String message, UserXtrCounters sender){
        if(message.length()<=1||message.toCharArray()[0]!='/') return;
        String data=message.toLowerCase().substring(1);
        int id = sender.getId();

        if(data.equals("likesonwall")){

            bot.sendMessage(id,""+sender.getFirstName()+" "+sender.getLastName()+", " +
                    "у тебя "+bot.calculateCountOfLikes(sender,"wall")+ emojies.get("heart")+" лайков под " +
                    "фотографиями на стене.");

        } else if(data.equals("likesonprofile")){

            bot.sendMessage(id,""+sender.getFirstName()+" "+sender.getLastName()+", " +
                    "у тебя "+bot.calculateCountOfLikes(sender,"profile")+ emojies.get("heart")+" лайков " +
                    "под фотографиями в профиле.");

        } else if(data.equals("totallikes")){

            bot.sendMessage(id,""+sender.getFirstName()+" "+sender.getLastName()+", " +
                    "у тебя "+bot.calculateContOfLikesOnPosts(sender)+ emojies.get("heart")+" лайков " +
                    "под записями на стене.");

        } else if(data.equals("btrate")){

            String[] btrate=bot.bitcoinRate();
            String dollar= emojies.get("dollar");
            bot.sendMessage(id,"Курс: "+btrate[0]+dollar+"\n"+
                    "Покупка: "+btrate[1]+dollar+"\n"+
                    "Продажа: "+btrate[2]+dollar);

        } else if(data.equals("fuckoff")){

            bot.sendMessage(id,sender.getFirstName()+" "+sender.getLastName()+", иди нахер"
                    + emojies.get("fuck"));

        } else if(data.matches("forecast.*")){

            String emojiLine= emojies.get("thermometer")+ emojies.get("sun")
                    + emojies.get("clWithLight")+ emojies.get("lightning")
                    + emojies.get("snowflake")+ emojies.get("cloud")
                    + emojies.get("clWithSnow")+ emojies.get("clWithRain")
                    + emojies.get("clWithSun")+ emojies.get("sunWithCl")
                    + emojies.get("sunWithClWithR");
            String[] mas=data.split(": ?");
            String[] input=mas.length==2?mas[1].split(", ?"):new String[0];
            String result=input.length==2?bot.receiveWeatherForecast(input[0].trim(),input[1].trim()):"";
            bot.sendMessage(id,result.equals("")?"Некорректный ввод.\nВвод должен быть" +
                    " в формате: \"Погода: <город (в именительном падеже)>, <код страны>\"":emojiLine+
                    "\n"+"\n"+result+emojiLine);

        } else if(data.matches("subway.*")){

            String[] mas=data.split(": ?");
            String[] parameters=mas.length==2?mas[1].split(", ?"):new String[0];
            String result=parameters.length==3?bot.calculateTimeInSubway(parameters[0].trim(),
                    parameters[1].trim(),parameters[2].trim()):"";
            bot.sendMessage(id,result.equals("")?"Некорректный ввод. Ввод должен быть в" +
                    " формате: \"Метро: <город>, <начальная станция>, <конечная станция>\"":result+ emojies
                    .get("watch"));

        } else if(data.matches("ai.*")){

            String[] input=data.split(": ");
            String response=input.length==2?bot.aiAnswer(input[1]):"Not correct command.";
            bot.sendMessage(id,response.equals("")?"Извини, я тебя не понял.":response);

        } else if(data.equals("stop")){

            bot.interruptLongPoll();
            bot.sendMessage(id,"VkBot has been stopped.");

        } else if(data.equals("start")){

            bot.startLongPoll();
            bot.sendMessage(id,"VkBot has been continued.");

        } else if(data.equals("exit")){

            bot.exit(0);

        } else if(data.equals("list")){

            bot.sendMessage(id,"=============List=============\n"+
                    "/likesOnWall\n/likesOnProfile\n/totalLikes\n/btRate\n/fuckOff\n" +
                    "/forecast: <city>, <country code>\n/ai: <query>\n" +
                    "/subway: <city>, <origin>, <destination>\n/stop\n/start\n/exit (deprecated)");

        }
    }
}