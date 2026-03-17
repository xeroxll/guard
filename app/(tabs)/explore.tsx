import { View, Text, ScrollView } from 'react-native';

export default function ExploreScreen() {
  return (
    <ScrollView className="flex-1 bg-white dark:bg-gray-900">
      <View className="px-6 pt-16 pb-8">
        <Text className="text-3xl font-bold text-gray-900 dark:text-white">
          Explore
        </Text>
        <Text className="mt-2 text-base text-gray-500 dark:text-gray-400">
          Discover what you can build with Expo.
        </Text>

        <View className="mt-8 gap-4">
          <View className="rounded-2xl bg-gray-50 p-5 dark:bg-gray-800">
            <Text className="text-lg font-semibold text-gray-900 dark:text-white">
              File-based Routing
            </Text>
            <Text className="mt-1 text-sm text-gray-500 dark:text-gray-400">
              Pages are defined in the app/ directory using Expo Router.
            </Text>
          </View>

          <View className="rounded-2xl bg-gray-50 p-5 dark:bg-gray-800">
            <Text className="text-lg font-semibold text-gray-900 dark:text-white">
              NativeWind Styling
            </Text>
            <Text className="mt-1 text-sm text-gray-500 dark:text-gray-400">
              Use Tailwind CSS classes for styling with the className prop.
            </Text>
          </View>

          <View className="rounded-2xl bg-gray-50 p-5 dark:bg-gray-800">
            <Text className="text-lg font-semibold text-gray-900 dark:text-white">
              Cross-Platform
            </Text>
            <Text className="mt-1 text-sm text-gray-500 dark:text-gray-400">
              Works on iOS, Android, and web from a single codebase.
            </Text>
          </View>
        </View>
      </View>
    </ScrollView>
  );
}
