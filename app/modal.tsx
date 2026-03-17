import { View, Text, Pressable } from 'react-native';
import { Link } from 'expo-router';

export default function ModalScreen() {
  return (
    <View className="flex-1 items-center justify-center bg-white p-5 dark:bg-gray-900">
      <Text className="text-2xl font-bold text-gray-900 dark:text-white">
        Modal
      </Text>
      <Text className="mt-2 text-center text-base text-gray-500 dark:text-gray-400">
        This is a modal screen.
      </Text>
      <Link href="/" dismissTo asChild>
        <Pressable className="mt-6 rounded-xl bg-blue-500 px-6 py-3">
          <Text className="text-base font-semibold text-white">
            Go back
          </Text>
        </Pressable>
      </Link>
    </View>
  );
}
