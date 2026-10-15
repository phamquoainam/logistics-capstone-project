import { render } from '@testing-library/react-native';
import App from '../App';

test('native starter renders its instruction after mount and remount', async () => {
  const instruction = 'Open up App.tsx to start working on your app!';
  const first = await render(<App />);
  expect(first.getByText(instruction)).toBeOnTheScreen();
  expect(first.queryByText(/uncaught|fatal|exception/i)).toBeNull();
  await first.unmount();
  const restarted = await render(<App />);
  expect(restarted.getAllByText(instruction)).toHaveLength(1);
});
