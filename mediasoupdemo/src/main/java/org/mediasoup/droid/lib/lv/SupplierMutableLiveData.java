package org.mediasoup.droid.lib.lv;


import androidx.core.util.Supplier;
import androidx.lifecycle.MutableLiveData;
import androidx.annotation.NonNull;


@SuppressWarnings("WeakerAccess")
public class SupplierMutableLiveData<T> extends MutableLiveData<T> {

  public SupplierMutableLiveData(@NonNull Supplier<T> supplier) {
    setValue(supplier.get());
  }

  @NonNull
  @Override
  @SuppressWarnings("all")
  public T getValue() {
    return super.getValue();
  }

  public interface Invoker<T> {
    void invokeAction(T value);
  }

  public void postValue(@NonNull Invoker<T> invoker) {
    T value = getValue();
    invoker.invokeAction(value);
    postValue(value);
  }
}
