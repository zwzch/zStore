package com.zstore.test;

import com.sun.xml.internal.ws.util.CompletedFuture;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class TestLang {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
//        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//           try {
//               TimeUnit.SECONDS.sleep(1);
//           } catch (InterruptedException e) {
//           }
//           if (new Random().nextInt() % 2 >= 0){
//               int i = 12/0;
//           }
//            System.out.println("run end ...");
//        });
//
//        future.whenComplete(new BiConsumer<Void, Throwable>() {
//            @Override
//            public void accept(Void aVoid, Throwable throwable) {
//                System.out.println("执行完成");
//            }
//        });
//        future.exceptionally(new Function<Throwable, Void>() {
//            @Override
//            public Void apply(Throwable throwable) {
//                System.out.println("执行失败！"+throwable.getMessage());
//                return null;
//            }
//        });
//        TimeUnit.SECONDS.sleep(2);

//        CompletableFuture<Long> future = CompletableFuture.supplyAsync(
//                new Supplier<Long>() {
//                    @Override
//                    public Long get() {
//                        long result = new Random().nextInt(100);
//                        System.out.println("result1="+result);
//                        return result;
//                    }
//                }
//        ).thenApply(new Function<Long, Long>() {
//            @Override
//            public Long apply(Long aLong) {
//                long result = aLong*5;
//                System.out.println("result2="+result);
//                return result;
//            }
//        });
//        long result = future.get();
//        System.out.println(result);

//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(new Supplier<Integer>() {
//            @Override
//            public Integer get() {
//                int i= 10/0;
//                return new Random().nextInt(10);
//            }
//        }).handle(new BiFunction<Integer, Throwable, Integer>() {
//            @Override
//            public Integer apply(Integer integer, Throwable throwable) {
//                int result = -1;
//                if(throwable==null){
//                    result = integer * 2;
//                }else{
//                    System.out.println(throwable.getMessage());
//                }
//                return result;
//            }
//        });

//        CompletableFuture<Void> future = CompletableFuture.supplyAsync(new Supplier<Integer>() {
//            @Override
//            public Integer get() {
//                return new Random().nextInt(10);
//            }
//        }).thenAccept(integer -> {
//            System.out.println(integer);
//        });
//        future.get();

//        CompletableFuture<Void> future = CompletableFuture.supplyAsync(new Supplier<Integer>() {
//            @Override
//            public Integer get() {
//                return new Random().nextInt(10);
//            }
//        }).thenRun(() -> {
//            System.out.println("thenRun...");
//        });
//        future.get();

//        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(new Supplier<String>() {
//            @Override
//            public String get() {
//                return "hello";
//            }
//        });
//        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(new Supplier<String>() {
//            @Override
//            public String get() {
//                return "hello";
//            }
//        });
//        CompletableFuture<String> result = future1.thenCombine(future2, new BiFunction<String, String, String>() {
//            @Override
//            public String apply(String t, String u) {
//                return t+" "+u;
//            }
//        });
//        System.out.println(result.get());

//        CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
//            @Override
//            public Integer get() {
//                int t = new Random().nextInt(3);
//                try {
//                    TimeUnit.SECONDS.sleep(t);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.println("f1="+t);
//                return t;
//            }
//        });
//
//        CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
//            @Override
//            public Integer get() {
//                int t = new Random().nextInt(3);
//                try {
//                    TimeUnit.SECONDS.sleep(t);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.println("f2="+t);
//                return t;
//            }
//        });
//        f1.thenAcceptBoth(f2, new BiConsumer<Integer, Integer>() {
//            @Override
//            public void accept(Integer t, Integer u) {
//                System.out.println("f1="+t+";f2="+u+";");
//            }
//        });
//        f1.get();
//        f2.get();

//        applyToEither choose fast future
//        CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
//            @Override
//            public Integer get() {
//                int t = new Random().nextInt(3);
//                try {
//                    TimeUnit.SECONDS.sleep(t);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.println("f1="+t);
//                return t;
//            }
//        });
//        CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
//            @Override
//            public Integer get() {
//                int t = new Random().nextInt(3);
//                try {
//                    TimeUnit.SECONDS.sleep(t);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.println("f2="+t);
//                return t;
//            }
//        });
//        CompletableFuture<Integer> result = f1.applyToEither(f2, new Function<Integer, Integer>() {
//            @Override
//            public Integer apply(Integer integer) {
//                System.out.println(integer);
//                return integer * 2;
//            }
//        });
//        System.out.println(result.get());

        //runAfterEither who complete while do
//        CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
//            @Override
//            public Integer get() {
//                int t = new Random().nextInt(3);
//                try {
//                    TimeUnit.SECONDS.sleep(t);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.println("f1="+t);
//                return t;
//            }
//        });
//
//        CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
//            @Override
//            public Integer get() {
//                int t = new Random().nextInt(3);
//                try {
//                    TimeUnit.SECONDS.sleep(t);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.println("f2="+t);
//                return t;
//            }
//        });
//
//        f1.runAfterEither(f2, new Runnable() {
//            @Override
//            public void run() {
//                System.out.println("上面有一个已经完成了。");
//            }
//        });

        //runAfterBoth two future
//        CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
//            @Override
//            public Integer get() {
//                int t = new Random().nextInt(3);
//                try {
//                    TimeUnit.SECONDS.sleep(t);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.println("f1="+t);
//                return t;
//            }
//        });
//
//        CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(new Supplier<Integer>() {
//            @Override
//            public Integer get() {
//                int t = new Random().nextInt(3);
//                try {
//                    TimeUnit.SECONDS.sleep(t);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.println("f2="+t);
//                return t;
//            }
//        });
//        f1.runAfterBoth(f2, new Runnable() {
//            @Override
//            public void run() {
//                System.out.println("上面两个任务都执行完成了。");
//            }
//        });
//        f1.get();
//        f2.get();

        //thenCompose
        CompletableFuture<Integer> f = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                int t = new Random().nextInt(3);
                System.out.println("t1="+t);
                return t;
            }
        }).thenCompose(new Function<Integer, CompletionStage<Integer>>() {
            @Override
            public CompletionStage<Integer> apply(Integer integer) {
                return CompletableFuture.supplyAsync(new Supplier<Integer>() {
                    @Override
                    public Integer get() {
                        int t = integer *2;
                        System.out.println("t2="+t);
                        return t;
                    }
                });
            }
        });
        System.out.println(f.get());
    }
}
