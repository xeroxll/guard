import React from 'react';
import { View, Text, SafeAreaView, FlatList } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useGuardian } from '@/hooks/useGuardian';

export default function LogScreen() {
  const { logs } = useGuardian();

  return (
    <SafeAreaView className="flex-1 bg-[#0a0b10]">
      <View className="px-6 py-4">
        <Text className="text-2xl font-bold text-white">Журнал</Text>
        <Text className="text-sm text-gray-400">История событий безопасности</Text>
      </View>

      <FlatList
        data={logs}
        contentContainerStyle={{ padding: 24 }}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <View className="mb-4 flex-row items-center rounded-3xl bg-[#12141c] p-4">
            <View className={`mr-4 h-12 w-12 items-center justify-center rounded-2xl ${
              item.type === 'block' ? 'bg-[#1a2a1a]' : 
              item.type === 'threat' ? 'bg-[#2a1a1a]' : 'bg-[#1a1a2a]'
            }`}>
              <MaterialCommunityIcons 
                name={item.type === 'block' ? 'shield' : item.type === 'threat' ? 'alert' : 'magnify'} 
                size={24} 
                color={item.type === 'block' ? '#22c55e' : item.type === 'threat' ? '#ef4444' : '#6366f1'} 
              />
            </View>
            <View className="flex-1">
              <Text className="text-base font-bold text-white">{item.title}</Text>
              <Text className="text-xs text-gray-500">{item.desc}</Text>
            </View>
            <Text className="text-xs text-gray-600">{item.time}</Text>
          </View>
        )}
        ListEmptyComponent={
          <View className="items-center justify-center py-20">
            <MaterialCommunityIcons name="history" size={64} color="#1a1c26" />
            <Text className="mt-4 text-gray-500">Журнал пуст</Text>
          </View>
        }
      />
    </SafeAreaView>
  );
}
