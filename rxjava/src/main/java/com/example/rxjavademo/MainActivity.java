package com.example.rxjavademo;

import android.app.AutomaticZenRule;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Notification;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.FuncN;
import rx.schedulers.Schedulers;
import rx.schedulers.TimeInterval;
import rx.schedulers.Timestamped;


public class MainActivity extends AppCompatActivity {
    private TextView message;
    private ImageView image;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        message = (TextView) findViewById(R.id.message);
        image = (ImageView) findViewById(R.id.image);
    }


    public void TestObservable(View view) {
        switch (view.getId()) {
            case R.id.just_Observable:
                justOperator();
                break;
            case R.id.create_observable:
                createOperator();
                break;
            case R.id.flatMap:
                flatOperator();
                break;
            case R.id.allAndAmb:
                allAndAmbOperator();
                break;
        }

    }

    private void allAndAmbOperator() {
        RXJavaAllTest().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                Log.v(TAG, "Observable.all()----all:" + aBoolean.toString());
            }
        });
        RXJavaAmbTest().subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {
                Log.v(TAG, "Observable.amb()----amb:" + integer);
            }
        });
    }

    /**
     * RXJava flatMap的使用
     */
    private void flatOperator() {
        ArrayList<String> mStrings = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            mStrings.add("string:" + i);
        }
        Observable.just(mStrings).subscribe(new Action1<ArrayList<String>>() {
            @Override
            public void call(ArrayList<String> strings) {
                Log.v(TAG, "flatOperator()---- Observable.subscribe() thread id:" + Thread.currentThread().getId());
                Observable.from(strings)
                        .subscribe(new Action1<String>() {
                            @Override
                            public void call(String s) {
                                Log.v("Observable_Thread", "flatOperator()---- Observable.subscribe() thread id:" + Thread.currentThread().getId() + "    string:" + s);
                            }
                        });
            }
        });
        Observable.just(mStrings).flatMap(new Func1<ArrayList<String>, Observable<String>>() {
            @Override
            public Observable<String> call(ArrayList<String> strings) {
                //根据实际需求 处理集合数据，并返回新的被观察者 将数据转换为需要的
                Log.v(TAG, "flatOperator()---- Observable.flatMap() thread id:" + Thread.currentThread().getId());
                return Observable.from(strings);
            }
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        Log.v(TAG, "flatOperator()---- Observable.subscribe() thread id:" + Thread.currentThread().getId() + "  String:" + s);
                    }
                });
    }

    /**
     * RXJava just输入 map转换 的使用
     */

    private void justOperator() {
        if (null != image.getDrawingCache()) {
            image.getDrawingCache().recycle();
        }
        image.setVisibility(View.GONE);
        setMessage("loading....");
        String url = "http://pic.4j4j.cn/upload/pic/20130909/681ebf9d64.jpg";
        Observable.just(url)
                .map(new Func1<String, Bitmap>() {
                    @Override
                    public Bitmap call(String s) {
                        Log.v(TAG, "justOperator()----map<String, Bitmap> thread id:" + Thread.currentThread().getId() + "    url：" + s);
                        return analysisBitmap(s);
                    }
                }).
                map(new Func1<Bitmap, Drawable>() {
                    @Override
                    public Drawable call(Bitmap bitmap) {
                        Log.v(TAG, "justOperator()----map<Bitmap, Drawable> thread id:" + Thread.currentThread().getId());
                        return new BitmapDrawable(bitmap);
                    }
                }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Drawable>() {
                    @Override
                    public void call(Drawable bitmap) {
                        Log.v(TAG, "justOperator()----subscribe thread name:" + Thread.currentThread().getName());
                        message.setVisibility(View.GONE);
                        image.setVisibility(View.VISIBLE);
                        image.setImageDrawable(bitmap);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {

                    }
                }, new Action0() {
                    @Override
                    public void call() {

                    }
                });
    }

    /**
     * RXJava  基本观察者 被观察者  订阅的使用
     */
    private void createOperator() {
        //被观察者
        Observable<String> mObservable = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {

                //注：此方法还是在主线程
                for (int i = 0; i < 20; i++) {
                    subscriber.onNext("i=" + i);
                }
                subscriber.onCompleted();
            }
        }).filter(new Func1<String, Boolean>() {
            @Override
            public Boolean call(String s) {
                return !s.contains("2");
            }
        }).take(5).doOnNext(new Action1<String>() {
            @Override
            public void call(String s) {
                Log.v(TAG, "thread name:" + Thread.currentThread().getName() + "    doOnNext:" + s);
            }
        });
        //观察者
        Subscriber<String> mSubscriber = new Subscriber<String>() {
            @Override
            public void onCompleted() {
                Log.v(TAG, "thread name:" + Thread.currentThread().getName() + "    onCompleted");
            }

            @Override
            public void onError(Throwable e) {
                Log.v(TAG, "thread name:" + Thread.currentThread().getName() + "    onError:" + e.getCause());
            }

            @Override
            public void onNext(String s) {
                Log.v(TAG, "thread name:" + Thread.currentThread().getName() + "    str:" + s);
            }
        };
        //订阅
        mObservable.subscribe(mSubscriber);
    }

    private Bitmap analysisBitmap(String url) {
        URL srcUrl = null;
        Bitmap mBitmap = null;
        //先解析Url
        try {
            srcUrl = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
//            Toast.makeText(this, "URl 解析失败", Toast.LENGTH_SHORT).show();
        }
        //解析Bitmap
        try {
            HttpURLConnection conn = (HttpURLConnection) srcUrl.openConnection();
//            conn.setDoOutput(true);
            conn.connect();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = conn.getInputStream();
                mBitmap = BitmapFactory.decodeStream(is);
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
//            setMessage("流传输失败");
        }
        return mBitmap;
    }

    /**
     * RXJava amb操作符的使用、
     * <p>
     * 对传入的被观察者，优先取用最先完成任务返回数据的被观察者，其他被观察者则抛弃
     *
     * @return
     */
    private Observable<Integer> RXJavaAmbTest() {
        Observable<Integer> delay1 = Observable.just(1, 2, 3).delay(3000, TimeUnit.MILLISECONDS);
        Observable<Integer> delay2 = Observable.just(4, 5, 6).delay(2000, TimeUnit.MILLISECONDS);
        Observable<Integer> delay3 = Observable.just(7, 8, 9).delay(1000, TimeUnit.MILLISECONDS);

        return Observable.amb(delay2, delay1, delay3);
    }

    private boolean tag = false;

    /**
     * RXJava  中all操作符的使用
     * <p>
     * 对输入的数据做统一的比较，当所有的数据都满足给出的条件时 才会返回true
     *
     * @return
     */
    private Observable<Boolean> RXJavaAllTest() {
        Observable<Integer> just;
        if (tag) {
            just = Observable.from(new ArrayList<Integer>() {
                {
                    for (int i = 0; i < 10; i++)
                        add(i);
                }

            });
//            just = Observable.just(1, 2, 3, 4, 5, 6, 7, 8);
        } else {
            just = Observable.from(new ArrayList<Integer>() {
                {
                    for (int i = 0; i < 20; i++)
                        add(i);
                }

            });
//            just = Observable.just(1, 2, 3, 4, 5);
        }
        tag = !tag;
        return just.all(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer integer) {
                return integer < 15;
            }
        });
    }

    /**
     * RXJava contains 使用
     */
    private void containsOperator() {
        RXJavaAmbTest().contains(3).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {

            }
        });
    }

    /**
     * RXJava isEmpty 对是否输出数据的判断
     */
    private void isEmptyOperator() {
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                subscriber.onCompleted();
            }
        }).isEmpty().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {

            }
        });
    }

    /**
     * RXJava defaultIfEmpty 对是否输出数据的判断
     */
    private void defaultIfEmptyOperator() {
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                subscriber.onCompleted();
            }
        }).defaultIfEmpty(20).subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {

            }
        });
    }

    /**
     * RXJava sequenceEqual的使用
     * 用来判断来个被观察者返回的数据是否相同(所谓相同是指 数据相同 顺序相同)
     */
    private void sequenceEqualOperator() {
        Observable<Integer> observable1 = Observable.just(1, 2, 3, 4, 5);
        Observable<Integer> observable2 = Observable.from(new ArrayList<Integer>() {{
            for (int i = 1; i < 6; i++) {
                add(i);
            }
        }});

        Observable.sequenceEqual(observable1, observable2).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {

            }
        });

    }

    /**
     * RXJava 中 对接收数据 时机进行控制的操作符
     * <p>
     * skipUntil、skipWhile、takeUntil、takeWhile
     */
    private void takeAndSkipOperator() {
        //skipUntil 当所观察的被观察者返回数据时 才开始处理数据
        //可以理解为skipUtil(src) 一直跳过直到（Until）src有反馈
        Observable.interval(1000, TimeUnit.MILLISECONDS).skipUntil(Observable.timer(3000, TimeUnit.MILLISECONDS)).subscribe(new Action1<Long>() {
            @Override
            public void call(Long aLong) {

            }
        });
        //skipWhile 当skipWhile所监视的函数 返回true则会一直跳过数据，不进行处理，
        // 函数返回false的时候 才开始处理数据
        //可以理解为 skipWhile(true);当是true的时候便会跳过
        Observable.interval(1000, TimeUnit.MILLISECONDS).skipWhile(new Func1<Long, Boolean>() {
            @Override
            public Boolean call(Long aLong) {
                return null;
            }
        });
        //takeWhile 基本理解同skipWhile
        //可以理解为 takeWhile(true) 当为true的时候会处理这个数据
        Observable.interval(1000, TimeUnit.MILLISECONDS).takeWhile(new Func1<Long, Boolean>() {
            @Override
            public Boolean call(Long aLong) {
                return null;
            }
        });
        //takeUntil 基本理解同skipUntil
        //可以理解为 takeUntil(src) 一直处理数据直到src有反馈，才会停止处理数据
        Observable.interval(1000, TimeUnit.MILLISECONDS).takeUntil(Observable.timer(3000, TimeUnit.MILLISECONDS)).subscribe(new Action1<Long>() {
            @Override
            public void call(Long aLong) {

            }
        });
        //takeUntil 基本理解同skipUntil
        //可以理解为 takeUntil(true) 为true时会在一直处理数据,直到为false才会停止处理数据
        Observable.interval(1000, TimeUnit.MILLISECONDS).takeUntil(new Func1<Long, Boolean>() {
            @Override
            public Boolean call(Long aLong) {
                return null;
            }
        }).subscribe(new Action1<Long>() {
            @Override
            public void call(Long aLong) {

            }
        });
    }

    /**
     * RXJava中 对事件延迟处理的操作符
     * <p>
     * delay 数据延迟反馈
     * delaySubscription 延时注册观察者
     */
    private void delayOperator() {
        createNewThreadRunnable().delay(2000, TimeUnit.MILLISECONDS).subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {

            }
        });
        createNewThreadRunnable().delaySubscription(2000, TimeUnit.MILLISECONDS).subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {

            }
        });
    }

    /**
     * RXJava  中doXXX操作符的使用
     * 主要是把Observable的生命周期实用接口 曝露出来 便于控制 或者中途对数据处理
     */
    private void doOperator() {
        createNewThreadRunnable()
                .doAfterTerminate(new Action0() {//Observable结束前的回调
                    @Override
                    public void call() {
                        //
                    }
                }).doOnCompleted(new Action0() {//Observable完成任务的时候
            @Override
            public void call() {

            }
        }).doOnEach(new Action1<Notification<? super Integer>>() {//Observable每次返回一个数据便会执行
            @Override
            public void call(Notification<? super Integer> notification) {
                Observable.just(notification).dematerialize().subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {

                    }
                });
            }
        }).doOnError(new Action1<Throwable>() {//Observable出错
            @Override
            public void call(Throwable throwable) {

            }
        }).doOnNext(new Action1<Integer>() {//Observable处理下一个数据之前
            @Override
            public void call(Integer integer) {

            }
        }).doOnRequest(new Action1<Long>() {
            @Override
            public void call(Long aLong) {

            }
        }).doOnSubscribe(new Action0() {//观察者、被观察者建立订阅关系的时候
            @Override
            public void call() {

            }
        }).doOnTerminate(new Action0() {//Observable结束的时候，包括因为出错结束，正常结束，手动结束
            @Override
            public void call() {

            }
        }).doOnUnsubscribe(new Action0() {//观察者、被观察者取消订阅关系的时候
            @Override
            public void call() {

            }
        }).finallyDo(new Action0() {
            @Override
            public void call() {

            }
        }).subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {

            }
        });
    }

    /**
     * RXJava 中materialize、dematerialize操作符的使用；
     * <p>
     * materialize将Observable的onNext()、onError()、onComplete() 封装为Notification对象作为返回数据 返回
     * dematerialize 则是相反的操作
     */
    private void materializeOperator() {
        materializeObservable().subscribe(new Action1<Notification<Integer>>() {
            @Override
            public void call(Notification<Integer> integerNotification) {

            }
        });
        deMaterializeObservable().subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {

            }
        });
    }

    /**
     * RXJAva  中对于Observable 与 subscriber所在线程的设置
     * <p>
     * subscribeOn 指定被观察者所在的线程，可以理解为对被观察者在那个线程订阅（订阅在XX线程）
     * observeOn 指定观察者所在的线程，即数据的返回后展示或者处理的线程 （我在XX线程接收数据）
     */
    private void ThreadOnOperator() {
        ArrayList<String> mStrings = new ArrayList<String>() {{
            for (int i = 0; i < 20; i++) {
                add("String : " + String.valueOf(i));
            }
        }};
        Observable.from(mStrings)
                .skipWhile(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        return s.contains("1");
                    }
                })
                .subscribeOn(Schedulers.io())
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        return !s.contains("0");
                    }
                }).subscribeOn(Schedulers.newThread())
                .map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
                        return "map:" + s;
                    }
                }).subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.immediate())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {

                    }
                });

    }

    /**
     * RXJava   中对时间的监听
     * timeInterval 对原来的数据 进行包装 加入时间，来标识与上一次数据返回时的间隔时间
     * timestamp  对原来的数据 进行包装 加入时间戳，来标识数据返回时的时刻
     * <p>
     * timeout 设置超时时间
     */
    private void TimeOperator() {
        createNewThreadRunnable()
                .timeInterval()
                .subscribe(new Action1<TimeInterval<Integer>>() {
                    @Override
                    public void call(TimeInterval<Integer> integerTimeInterval) {
                        integerTimeInterval.getIntervalInMilliseconds();
                        integerTimeInterval.getValue();
                    }
                });
        createNewThreadRunnable()
                .timestamp()
                .subscribe(new Action1<Timestamped<Integer>>() {
                    @Override
                    public void call(Timestamped<Integer> integerTimestamped) {
                        integerTimestamped.getTimestampMillis();
                    }
                });

        createTimeOutObservable()
                .timeout(3000, TimeUnit.MILLISECONDS, Observable.just(1, 2, 3, 4, 5, 6, 7), Schedulers.immediate())
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {

                    }
                });
    }

    /**
     * RXJava using 操作符的使用
     * <p>
     * 使用给定的资源，并根据资源的变量来动态创建Observable
     *
     * @see Observable#using(Func0, Func1, Action1)
     * @see Observable#using(Func0, Func1, Action1, boolean)
     * Func0  获取所需要使用的对象 返回给Observable
     * Func1  根据对象 处理后返回Observable;
     * Action1 资源销毁时的回调
     * boolean
     */
    private void usingOperator(final InnerObject object) {
        Observable.using(new Func0<InnerObject>() {
            @Override
            public InnerObject call() {
                return object;
            }
        }, new Func1<InnerObject, Observable<Long>>() {
            @Override
            public Observable<Long> call(InnerObject innerObject) {
                return Observable.timer(5000, TimeUnit.MILLISECONDS);
            }
        }, new Action1<InnerObject>() {
            @Override
            public void call(InnerObject innerObject) {
                if (innerObject != null) {
                    innerObject.release();
                }
            }
        }).subscribe(new Action1<Long>() {
            @Override
            public void call(Long aLong) {

            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {

            }
        }, new Action0() {
            @Override
            public void call() {
            }
        }).unsubscribe();
    }

    /**
     * RXJava 中对Exception的捕获与处理
     * <p>
     * <p>
     * onErrorReturn {@see Observable#onErrorReturn(Func1) }
     * Error发生时,返回预定义好的数据
     * <p>
     * onErrorResumeNext{@see Observable#onErrorResumeNext(Func1)}
     * Error发生时，可根据Throwable，来判断需要使用的Observable
     * <p>
     * onErrorResumeNext{@see Observable#onErrorResumeNext(Observable) }
     * Error发生时，使用另一个Observable继续处理数据
     * <p>
     * onExceptionResumeNext{@see Observable#onExceptionResumeNext(Observable)}
     * 如果是Exception,则会使用指定的Observable进行数据的继续处理
     * 否则 会将错误分发至Subscribe
     */
    private void errorHandingOperator() {
        createExceptionObservable()
                .onErrorReturn(new Func1<Throwable, Integer>() {
                    @Override
                    public Integer call(Throwable throwable) {
                        return 100;
                    }
                }).subscribe(new CustomSubscribe<Integer>("onErrorReturn"));
        createExceptionObservable()
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Integer>>() {
                    @Override
                    public Observable<? extends Integer> call(Throwable throwable) {
                        return Observable.just(100, 101, 102);
                    }
                }).subscribe(new CustomSubscribe<Integer>("onErrorResumeNext"));

        createExceptionObservable()
                .onExceptionResumeNext(Observable.just(200, 201, 202))
                .subscribe(new CustomSubscribe<Integer>("onExceptionResumeNext"));

    }

    /**
     * RXJava 中发生错误时，重新建立订阅关系  操作符的使用
     * <p>
     * retry{
     *
     * @see Observable#retry() 会在每次出现错误时，都重新订阅，会出现无限循环的情况
     * @see Observable#retry(Func2)  Func2 可以根据错误类型 与重复次数 来控制是否不再重新订阅 而是将错误返回给观察者
     * @see Observable#retry(long) long 尝试的次数 如果仍然有错，则会将错误返回给观察者
     * }
     * retryWhen{
     * @see Observable#retryWhen(Func1) Func1 返回一个创建好的Observable 当有数据返回时便重新订阅
     * @see Observable#retryWhen(Func1, Scheduler) 指定作为判断节点的Observable运行在那个线程
     * }
     */
    private void retryOperator() {
        createExceptionObservable().retry(new Func2<Integer, Throwable, Boolean>() {
            @Override
            public Boolean call(Integer integer, Throwable throwable) {
                return true;
            }
        }).subscribe(new CustomSubscribe<Integer>("retry-with_func2"));
        createExceptionObservable().retryWhen(new Func1<Observable<? extends Throwable>, Observable<Long>>() {
            @Override
            public Observable<Long> call(Observable<? extends Throwable> observable) {
                return Observable.timer(2000, TimeUnit.MILLISECONDS);
            }
        }).subscribe(new CustomSubscribe<Integer>("retryWhen-no-scheduler"));
        createExceptionObservable().retryWhen(new Func1<Observable<? extends Throwable>, Observable<Long>>() {
            @Override
            public Observable<Long> call(Observable<? extends Throwable> observable) {
                return Observable.timer(2000, TimeUnit.MILLISECONDS);
            }
        }, Schedulers.immediate()).subscribe(new CustomSubscribe<Integer>("retryWhen-scheduler"));
    }

    /**
     * RXJava combineLatest的用法
     * <p>
     * 使用list传值
     *
     * @see Observable#combineLatest(Iterable, FuncN)
     * @see Observable#combineLatest(List, FuncN)
     * <p>
     * 直接将Observable作为参数
     * @see Observable#combineLatest(Observable, Observable, Func2)
     * 错误将在 操作结束后处理
     * @see Observable#combineLatestDelayError(Iterable, FuncN)
     */
    private void CombineLatestOperator() {
        Observable.combineLatest(createObservable(1), createObservable(2), new Func2<String, String, String>() {
            @Override
            public String call(String s, String s2) {
                return null;
            }
        }).subscribe(new CustomSubscribe<String>("combineLatest-Observable"));
        ArrayList<Observable<String>> mObservables = new ArrayList<Observable<String>>() {
            {
                for (int i = 10; i < 25; i++) {
                    add(createObservable(i));
                }
            }
        };
        Observable.combineLatest(mObservables, new FuncN<String>() {
            @Override
            public String call(Object... args) {
                String result = "";
                for (Object mArg : args) {
                    result = result + " " + String.valueOf(mArg);
                }
                return result;
            }
        }).subscribe(new CustomSubscribe<String>("combineLatest-List"));
    }

    /**
     * RXJava  中join相关操作符的使用
     *
     * @see Observable#join(Observable, Func1, Func1, Func2)
     * @see Observable#groupJoin(Observable, Func1, Func1, Func2)
     * <p>
     * Observable 目标Observable
     * Func1 接收源Observable的数据，并返回一个Observable
     * Func1  接收目标Observable的数据，并返回一个Observable
     * Func2 处理源Observable与目标Observable的数据，并返回组合的数据
     */
    private void joinOperator() {
        Observable.just("left-").join(createObservable("right-"), new Func1<String, Observable<Long>>() {
            @Override
            public Observable<Long> call(String s) {
                return Observable.timer(2, TimeUnit.SECONDS);
            }
        }, new Func1<String, Observable<Long>>() {
            @Override
            public Observable<Long> call(String s) {
                return Observable.timer(3, TimeUnit.SECONDS);
            }
        }, new Func2<String, String, String>() {
            @Override
            public String call(String s, String s2) {
                return s + "  " + s2;
            }
        }).subscribe(new CustomSubscribe<String>("join-"));

        Observable.just("left-").groupJoin(createObservable("right-"), new Func1<String, Observable<Long>>() {
            @Override
            public Observable<Long> call(String s) {
                return Observable.timer(2, TimeUnit.SECONDS);
            }
        }, new Func1<String, Observable<Long>>() {
            @Override
            public Observable<Long> call(String s) {
                return Observable.timer(3, TimeUnit.SECONDS);
            }
        }, new Func2<String, Observable<String>, Observable<String>>() {
            @Override
            public Observable<String> call(final String s, Observable<String> stringObservable) {
                return stringObservable.map(new Func1<String, String>() {
                    @Override
                    public String call(String str) {
                        return s + str;
                    }
                });
            }
        }).subscribe(new Action1<Observable<String>>() {
            @Override
            public void call(Observable<String> stringObservable) {
                stringObservable.subscribe(new CustomSubscribe<String>());
            }
        });
    }


    private void setMessage(String str) {
        message.setVisibility(View.VISIBLE);
        message.setText(str);
    }

    /**
     * 创建一个有延迟效果的被观察者
     *
     * @return
     */
    private Observable<Integer> createNewThreadRunnable() {
        return Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                for (int i = 0; i < 20; i++) {
                    subscriber.onNext(i);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).subscribeOn(Schedulers.newThread());
    }

    /**
     * 创建一个会报错的被观察者
     *
     * @return
     */
    private Observable<Integer> createExceptionObservable() {
        return Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                for (int i = 0; i < 20; i++) {
                    if (i % 3 == 0) {
                        subscriber.onError(new Throwable("Throwable " + i));
                    } else {
                        subscriber.onNext(i);
                    }
                }
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.newThread());
    }

    /**
     * 创建一个会逐步增大睡眠时间，导致超时的被观察者，Observable
     *
     * @return
     */
    private Observable<Integer> createTimeOutObservable() {
        return Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                for (int i = 0; i < 20; i++) {
                    subscriber.onNext(i);
                    try {
                        Thread.sleep(1000 + 500 * i);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).subscribeOn(Schedulers.newThread());
    }

    private Observable<String> createObservable(final int index) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                for (int i = 0; i < 10; i++) {
                    subscriber.onNext("index:" + index + "  i:" + i);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).subscribeOn(Schedulers.newThread());
    }

    private Observable<String> createObservable(final String name) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                for (int i = 0; i < 10; i++) {
                    subscriber.onNext(name + "  i:" + i);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).subscribeOn(Schedulers.newThread());
    }

    /**
     * @return
     */
    private Observable<Notification<Integer>> materializeObservable() {
        return Observable.just(1, 2, 3, 4, 5, 6).materialize();
    }

    /**
     * @return
     */
    private Observable<Integer> deMaterializeObservable() {
        return materializeObservable().dematerialize();
    }

    /**
     *
     */
    private class InnerObject {

        CustomSubscribe<Long> mSubscribe = new CustomSubscribe<Long>() {
            @Override
            public void onNext(Long o) {
                super.onNext(o);
            }
        };

        public InnerObject() {
            Observable.interval(1000, TimeUnit.MILLISECONDS).subscribe(mSubscribe);
        }

        public void release() {
            mSubscribe.unsubscribe();
        }
    }

    private class CustomSubscribe<T> extends Subscriber<T> {
        private String TAG = "default";

        public CustomSubscribe(String TAG) {
            this.TAG = TAG;
        }

        public CustomSubscribe() {
        }

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onNext(Object o) {

        }
    }
}
