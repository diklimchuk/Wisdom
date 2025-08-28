import com.vkontakte.wisdom.Option
import kotlin.reflect.typeOf
import kotlin.test.Test

class Tmp {

    @Test
    fun tmp() {
        val test = typeOf<Option<Int>>().toString()
        test.plus("")
    }
}