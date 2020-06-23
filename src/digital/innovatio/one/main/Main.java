package digital.innovatio.one.main;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {

    static ExecutorService executor = Executors.newFixedThreadPool(6, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            System.out.println("Nova Thread criada" + (thread.isDaemon() ? "daemon" : "") + " Thread Group :: " + thread.getThreadGroup());
            return thread;
        }
    });

    public static void main(String[] args) throws IOException, InterruptedException {
        //connectAndPrintURLJava();
        connectAkamaiHttpClient();
    }

    private static void connectAkamaiHttpClient() throws IOException, InterruptedException {
        System.out.println("Running HTTP/1.1 example ...");

        try{
            HttpClient httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .proxy(ProxySelector.getDefault())
                    .build();

            long start = System.currentTimeMillis();

            HttpRequest mainRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://http2.akamai.com/demo/h2_demo_frame.html"))
                    .build();

            HttpResponse<String> response = httpClient.send(mainRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Status code ::: " + response.statusCode());
            System.out.println("Headers response ::: " + response.headers());
            String responseBody = response.body();

            List<Future<?>> future = new ArrayList<>();

            responseBody
                    .lines()
                    .filter(line -> line.trim().startsWith("<img height"))
                    .map(line -> line.substring(line.indexOf("src='")+5, line.indexOf("'/>")))
                    .forEach(image -> {
                        Future<?> imgFuture = executor.submit(() -> {
                           HttpRequest imgRequest = HttpRequest.newBuilder()
                                   .uri(URI.create("https://http2.akamai.com"+image))
                                   .build();

                           try{
                               HttpResponse<String> imgResponse = httpClient.send(imgRequest, HttpResponse.BodyHandlers.ofString());
                               System.out.println("Imagem Carregada :: "+ image+ ", status code :: "+imgResponse.statusCode());
                           } catch (InterruptedException | IOException e){
                               System.out.println("Mensagem de error durante requisiçao para recuperar imagem"+image);;
                           }

                        });
                        future.add(imgFuture);
                        System.out.println("Submetido um futuro para imagem :: "+ image);
                    });

            future.forEach(f -> {
                try{
                    f.get();
                } catch (InterruptedException | ExecutionException e){
                    System.out.println("Error ao carregar imagem do futuro");
                }
            });

            long end = System.currentTimeMillis();
            System.out.println("Tempo de carregamento total :: " + (end -start)+ "ms" );

        } finally {
            executor.shutdown();
        }
    }

    private static void connectAndPrintURLJava() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET().uri(URI.create("https://www.google.com/"))
                .build();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status code :: " + response.statusCode());
        System.out.println("Headers response :: " + response.headers());
        System.out.println(response.body());
    }
}
